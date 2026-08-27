package com.example.service.intelligence

import android.util.Log
import com.example.data.local.StudyMateDatabase
import com.example.data.model.*
import com.example.data.repository.ExamQuestionBankRepository
import com.example.data.remote.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

enum class PracticeMode(val displayName: String, val description: String, val defaultQuestions: Int) {
    QUICK_PRACTICE("Quick Practice", "Short session with 5-10 rapid questions from active subjects", 5),
    TOPIC_PRACTICE("Topic Practice", "Deep dive into a specific topic from your syllabus", 10),
    SUBJECT_PRACTICE("Subject Practice", "Broad practice across your selected subject", 15),
    REVISION_PRACTICE("Revision Practice", "Targeted review of past mistakes and revision queue", 10),
    MOCK_TEST("Mock Test", "Full exam blueprint with timer, sections & optional negative marking", 20),
    WEAK_AREA_PRACTICE("Weak-Area Practice", "Focus on lower-performing topics based strictly on attempt accuracy", 10),
    SAVED_QUESTIONS("Saved Questions", "Practice questions you have saved or bookmarked for review", 10)
}

data class PracticeCurationResult(
    val session: PracticeSessionEntity,
    val questions: List<Question>,
    val noticeMessage: String? = null
)

class PracticeEngineService(
    private val database: StudyMateDatabase,
    private val questionBankRepo: ExamQuestionBankRepository = ExamQuestionBankRepository(),
    private val geminiRepo: GeminiRepository = GeminiRepository()
) {
    companion object {
        private const val TAG = "PracticeEngineService"
        val SUPPORTED_LANGUAGES = setOf("English", "Hindi", "Hinglish")
        val VALID_DIFFICULTIES = setOf("Easy", "Medium", "Hard", "Mixed")
    }

    val allPracticeSessions: Flow<List<PracticeSessionEntity>> = database.practiceDao().getAllPracticeSessions()
    val allQuestionAttempts: Flow<List<QuestionAttemptEntity>> = database.practiceDao().getAllQuestionAttempts()
    val allSavedQuestions: Flow<List<SavedQuestionEntity>> = database.practiceDao().getAllSavedQuestions()

    /**
     * Strict Question Validation.
     * Rejects malformed or incomplete questions before publishing.
     */
    fun validateQuestion(q: Question): Boolean {
        if (q.questionText.isBlank() || q.questionText.trim().length < 8) return false
        if (q.options.size < 2) return false
        if (q.options.any { it.isBlank() }) return false
        if (q.correctOptionIndex !in 0 until q.options.size) return false
        if (q.explanation.isBlank() || q.explanation.trim().length < 5) return false
        if (q.subject.isBlank()) return false
        if (q.topic.isBlank()) return false
        if (q.status == "DISABLED" || q.status == "REPORTED") return false
        return true
    }

    /**
     * Filters, validates, and deduplicates candidate questions.
     */
    fun validateAndPublishQuestions(candidates: List<Question>): List<Question> {
        val valid = candidates.filter { validateQuestion(it) }
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Question>()

        for (q in valid) {
            val normalized = q.questionText.lowercase().replace(Regex("[^a-z0-9]"), "")
            if (normalized.length >= 6 && !seen.contains(normalized)) {
                seen.add(normalized)
                result.add(q)
            }
        }
        return result
    }

    /**
     * Prepares and creates a practice session for any supported PracticeMode.
     */
    suspend fun createPracticeSession(
        mode: PracticeMode,
        examName: String = "General Exam",
        subject: String = "",
        topic: String = "",
        desiredQuestionCount: Int = 10,
        language: String = "English"
    ): PracticeCurationResult = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()
        val count = desiredQuestionCount.coerceIn(5, 50)

        val candidateQuestions = when (mode) {
            PracticeMode.QUICK_PRACTICE -> {
                val bank = questionBankRepo.getQuestionsForTest(
                    examName = examName,
                    subject = subject,
                    topic = "All Topics",
                    difficulty = "Mixed",
                    language = language,
                    desiredCount = count * 2
                )
                validateAndPublishQuestions(bank).shuffled().take(count)
            }

            PracticeMode.TOPIC_PRACTICE -> {
                val bank = questionBankRepo.getQuestionsForTest(
                    examName = examName,
                    subject = subject,
                    topic = topic,
                    difficulty = "Mixed",
                    language = language,
                    desiredCount = count * 2
                )
                validateAndPublishQuestions(bank).take(count)
            }

            PracticeMode.SUBJECT_PRACTICE -> {
                val bank = questionBankRepo.getQuestionsForTest(
                    examName = examName,
                    subject = subject,
                    topic = "All Topics",
                    difficulty = "Mixed",
                    language = language,
                    desiredCount = count * 2
                )
                validateAndPublishQuestions(bank).take(count)
            }

            PracticeMode.REVISION_PRACTICE -> {
                val mistakes = database.mistakeDao().getUnmasteredMistakesByExam(examName).firstOrNull() ?: emptyList()
                val mistakeQs = mistakes.map { m ->
                    Question(
                        id = "mistake_${m.id}",
                        questionText = m.questionText,
                        options = listOf(m.correctAnswer, m.studentAnswer, "None of the above", "Both A and B").shuffled(),
                        correctOptionIndex = 0, // Option matches correct answer
                        explanation = m.explanation,
                        subject = m.subject,
                        topic = m.topic,
                        source = QuestionSource.USER_CREATED,
                        sourceLabel = "Mistake Revision Queue",
                        difficulty = "Medium",
                        language = language
                    )
                }
                val bank = questionBankRepo.getQuestionsForTest(
                    examName = examName,
                    subject = subject,
                    topic = "All Topics",
                    difficulty = "Mixed",
                    language = language,
                    desiredCount = count
                )
                validateAndPublishQuestions(mistakeQs + bank).take(count)
            }

            PracticeMode.WEAK_AREA_PRACTICE -> {
                val attempts = database.practiceDao().getAllQuestionAttemptsOnce()
                val topicStats = attempts.groupBy { it.topic }
                    .mapValues { (_, atts) ->
                        val correct = atts.count { it.isCorrect }
                        val total = atts.size
                        val accuracy = if (total > 0) (correct.toFloat() / total) * 100f else 0f
                        Pair(total, accuracy)
                    }

                val weakTopics = topicStats.filter { it.value.first >= 2 && it.value.second < 60f }.keys
                if (weakTopics.isEmpty() && attempts.size < 5) {
                    // Insufficient attempt data
                    val bank = questionBankRepo.getQuestionsForTest(
                        examName = examName,
                        subject = subject,
                        topic = "All Topics",
                        difficulty = "Mixed",
                        language = language,
                        desiredCount = count
                    )
                    return@withContext PracticeCurationResult(
                        session = PracticeSessionEntity(
                            practiceSessionId = sessionId,
                            examId = examName,
                            subjectId = subject,
                            topicId = topic,
                            mode = mode.name,
                            questionCount = count
                        ),
                        questions = validateAndPublishQuestions(bank).take(count),
                        noticeMessage = "Not enough practice data yet. Showing introductory practice questions."
                    )
                }

                val targetTopic = weakTopics.firstOrNull() ?: topic.ifBlank { "Core Concepts" }
                val bank = questionBankRepo.getQuestionsForTest(
                    examName = examName,
                    subject = subject,
                    topic = targetTopic,
                    difficulty = "Mixed",
                    language = language,
                    desiredCount = count * 2
                )
                validateAndPublishQuestions(bank).take(count)
            }

            PracticeMode.SAVED_QUESTIONS -> {
                val savedEntities = database.practiceDao().getAllSavedQuestionsOnce()
                if (savedEntities.isEmpty()) {
                    val bank = questionBankRepo.getQuestionsForTest(
                        examName = examName,
                        subject = subject,
                        topic = "All Topics",
                        difficulty = "Mixed",
                        language = language,
                        desiredCount = count
                    )
                    return@withContext PracticeCurationResult(
                        session = PracticeSessionEntity(
                            practiceSessionId = sessionId,
                            examId = examName,
                            subjectId = subject,
                            topicId = topic,
                            mode = mode.name,
                            questionCount = count
                        ),
                        questions = validateAndPublishQuestions(bank).take(count),
                        noticeMessage = "No saved questions yet. Complete practice and save questions to see them here!"
                    )
                }

                val savedQs = savedEntities.map { s ->
                    val opts = if (s.optionsJson.isNotBlank()) {
                        runCatching {
                            s.optionsJson.split("|||").filter { it.isNotBlank() }
                        }.getOrDefault(listOf("Option A", "Option B", "Option C", "Option D"))
                    } else listOf("Option A", "Option B", "Option C", "Option D")

                    Question(
                        id = s.questionId,
                        questionText = s.questionText,
                        options = opts,
                        correctOptionIndex = s.correctOptionIndex,
                        explanation = s.explanation,
                        subject = s.subject,
                        topic = s.topic,
                        source = QuestionSource.APP_CURATED,
                        sourceLabel = "Saved Questions",
                        language = language
                    )
                }
                validateAndPublishQuestions(savedQs).take(count)
            }

            PracticeMode.MOCK_TEST -> {
                val bank = questionBankRepo.getQuestionsForTest(
                    examName = examName,
                    subject = subject,
                    topic = "All Topics",
                    difficulty = "Mixed",
                    language = language,
                    desiredCount = count * 2
                )
                validateAndPublishQuestions(bank).take(count)
            }
        }

        val sessionEntity = PracticeSessionEntity(
            practiceSessionId = sessionId,
            examId = examName,
            subjectId = subject.ifBlank { candidateQuestions.firstOrNull()?.subject ?: "General" },
            topicId = topic.ifBlank { candidateQuestions.firstOrNull()?.topic ?: "Core" },
            mode = mode.name,
            questionCount = candidateQuestions.size,
            startedAt = System.currentTimeMillis(),
            status = "IN_PROGRESS"
        )

        database.practiceDao().insertPracticeSession(sessionEntity)

        PracticeCurationResult(
            session = sessionEntity,
            questions = candidateQuestions
        )
    }

    /**
     * Records an attempt for a question during a practice session.
     */
    suspend fun recordAttempt(
        sessionId: String,
        question: Question,
        selectedIndex: Int?,
        timeSpentSeconds: Int
    ) = withContext(Dispatchers.IO) {
        val isCorr = selectedIndex != null && selectedIndex == question.correctOptionIndex
        val selectedText = if (selectedIndex != null && selectedIndex in 0 until question.options.size) {
            question.options[selectedIndex]
        } else "Skipped"

        val attemptEntity = QuestionAttemptEntity(
            practiceSessionId = sessionId,
            questionId = question.id,
            selectedAnswer = selectedText,
            isCorrect = isCorr,
            timeSpentSeconds = timeSpentSeconds,
            attemptedAt = System.currentTimeMillis(),
            examId = question.examName,
            subject = question.subject,
            topic = question.topic,
            difficulty = question.difficulty
        )

        database.practiceDao().insertQuestionAttempt(attemptEntity)

        // Update Question History
        val existingHistory = database.questionHistoryDao().getHistoryForQuestion(question.id)
        if (existingHistory != null) {
            val updated = existingHistory.copy(
                attemptCount = existingHistory.attemptCount + 1,
                correctCount = if (isCorr) existingHistory.correctCount + 1 else existingHistory.correctCount,
                incorrectCount = if (!isCorr && selectedIndex != null) existingHistory.incorrectCount + 1 else existingHistory.incorrectCount,
                lastAttemptedAt = System.currentTimeMillis(),
                lastResult = if (isCorr) "CORRECT" else if (selectedIndex == null) "SKIPPED" else "INCORRECT",
                lastResponseTimeSecs = timeSpentSeconds
            )
            database.questionHistoryDao().insertOrUpdateHistory(updated)
        } else {
            val newHistory = QuestionHistoryEntity(
                id = "${question.id}_history",
                questionId = question.id,
                examId = question.examName,
                subject = question.subject,
                topic = question.topic,
                attemptCount = 1,
                correctCount = if (isCorr) 1 else 0,
                incorrectCount = if (!isCorr && selectedIndex != null) 1 else 0,
                lastAttemptedAt = System.currentTimeMillis(),
                lastResult = if (isCorr) "CORRECT" else if (selectedIndex == null) "SKIPPED" else "INCORRECT",
                lastResponseTimeSecs = timeSpentSeconds
            )
            database.questionHistoryDao().insertOrUpdateHistory(newHistory)
        }

        // Handle Mistake Queue
        if (!isCorr && selectedIndex != null) {
            val mistake = MistakeItem(
                examId = question.examName,
                subject = question.subject,
                topic = question.topic,
                questionText = question.questionText,
                studentAnswer = selectedText,
                correctAnswer = question.correctAnswer,
                explanation = question.explanation,
                timestamp = System.currentTimeMillis(),
                isMastered = false
            )
            database.mistakeDao().insertMistake(mistake)
        } else if (isCorr) {
            val existingMistakes = database.mistakeDao().getUnmasteredMistakesForTopic(question.examName, question.topic)
            val matching = existingMistakes.firstOrNull { it.questionText == question.questionText }
            if (matching != null) {
                database.mistakeDao().updateMastered(matching.id, true)
            }
        }
    }

    /**
     * Finishes and computes final results for a practice session.
     */
    suspend fun finishPracticeSession(
        sessionId: String,
        score: Float,
        accuracyPercent: Float,
        timeSpentSeconds: Int
    ) = withContext(Dispatchers.IO) {
        val session = database.practiceDao().getPracticeSessionById(sessionId) ?: return@withContext
        val updated = session.copy(
            completedAt = System.currentTimeMillis(),
            status = "COMPLETED",
            score = score,
            accuracyPercent = accuracyPercent,
            timeSpentSeconds = timeSpentSeconds
        )
        database.practiceDao().updatePracticeSession(updated)
    }

    /**
     * Save or unsave a question for revision.
     */
    suspend fun saveQuestion(question: Question) = withContext(Dispatchers.IO) {
        val entity = SavedQuestionEntity(
            id = question.id,
            questionId = question.id,
            questionText = question.questionText,
            optionsJson = question.options.joinToString("|||"),
            correctOptionIndex = question.correctOptionIndex,
            explanation = question.explanation,
            subject = question.subject,
            topic = question.topic,
            source = question.source.name,
            savedAt = System.currentTimeMillis()
        )
        database.practiceDao().saveQuestion(entity)
    }

    suspend fun unsaveQuestion(questionId: String) = withContext(Dispatchers.IO) {
        database.practiceDao().unsaveQuestion(questionId)
    }

    /**
     * Report a potentially incorrect or unclear question.
     */
    suspend fun reportQuestion(
        questionId: String,
        examId: String = "",
        reason: String,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val report = QuestionQualityReportEntity(
            questionId = questionId,
            examId = examId,
            reason = reason,
            notes = notes,
            status = "UNDER_REVIEW",
            timestamp = System.currentTimeMillis()
        )
        database.questionQualityReportDao().insertReport(report)
    }

    /**
     * Calculates topic accuracy based on actual attempt history.
     */
    suspend fun calculateTopicAccuracy(subject: String, topic: String): Pair<Int, Float> = withContext(Dispatchers.IO) {
        val attempts = database.practiceDao().getAttemptsForTopic(subject, topic)
        if (attempts.isEmpty()) return@withContext Pair(0, 0f)

        val total = attempts.size
        val correct = attempts.count { it.isCorrect }
        val accuracy = (correct.toFloat() / total) * 100f
        Pair(total, accuracy)
    }

    /**
     * Calculates subject accuracy based on actual attempt history.
     */
    suspend fun calculateSubjectAccuracy(subject: String): Pair<Int, Float> = withContext(Dispatchers.IO) {
        val attempts = database.practiceDao().getAttemptsForSubject(subject)
        if (attempts.isEmpty()) return@withContext Pair(0, 0f)

        val total = attempts.size
        val correct = attempts.count { it.isCorrect }
        val accuracy = (correct.toFloat() / total) * 100f
        Pair(total, accuracy)
    }
}
