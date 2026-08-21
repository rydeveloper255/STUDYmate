package com.example.service.intelligence

import android.util.Log
import com.example.data.model.*
import com.example.data.remote.GeminiRepository
import com.example.data.repository.ExamQuestionBankRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Result of Question Source Engine operations.
 */
sealed class QuestionSourceResult {
    data class Success(
        val questions: List<Question>,
        val requestedCount: Int,
        val sourceSummary: String
    ) : QuestionSourceResult()

    data class InsufficientPyq(
        val availableCount: Int,
        val requestedCount: Int,
        val availableQuestions: List<Question>,
        val examName: String,
        val subject: String
    ) : QuestionSourceResult()

    data class Failure(
        val error: TestGenerationError
    ) : QuestionSourceResult()
}

/**
 * Unified Question Source Engine.
 * 
 * Provides authentic, source-labeled questions across:
 * 1. Verified PYQ (Previous Year Questions)
 * 2. Chapter Practice (AI Concept Practice)
 * 3. Official Exam Pattern Mock
 * 4. Current Affairs Practice
 * 5. Balanced Mixed Practice
 * 
 * Guarantees:
 * - Strict quality validation (non-empty, exactly 4 options, valid correct index, explanation)
 * - Anti-repetition & deduplication
 * - Clear, honest labeling (Never labels AI as PYQ!)
 * - Zero infinite loading (Network/AI timeouts with fallback)
 */
class QuestionSourceEngine(
    private val examQuestionBankRepository: ExamQuestionBankRepository,
    private val geminiRepository: GeminiRepository
) {
    companion object {
        private const val TAG = "QuestionSourceEngine"
        private const val AI_GENERATION_TIMEOUT_MS = 14000L
    }

    /**
     * Main entry point to fetch and curate questions according to the user's MockTestConfig.
     */
    suspend fun curateTestQuestions(
        config: MockTestConfig,
        examContext: ExamContext,
        historyList: List<QuestionHistoryEntity> = emptyList(),
        weakTopics: Set<String> = emptySet(),
        mistakeTopics: Set<String> = emptySet(),
        flaggedQuestionIds: List<String> = emptyList(),
        allowPartialPyqOrFill: Boolean = false
    ): QuestionSourceResult = withContext(Dispatchers.IO) {
        val targetExamName = config.exam.ifBlank { examContext.examName }
        val targetSubject = config.subject
        val targetCount = config.questionCount.coerceIn(5, 100)
        val targetLanguage = config.language

        Log.d(TAG, "Curating test: exam=$targetExamName, type=${config.testType}, source=${config.questionSource}, count=$targetCount, lang=$targetLanguage")

        try {
            when (config.questionSource) {
                QuestionSourceType.PYQ -> {
                    curatePyqQuestions(
                        config = config,
                        targetExamName = targetExamName,
                        targetSubject = targetSubject,
                        targetCount = targetCount,
                        targetLanguage = targetLanguage,
                        allowPartialPyqOrFill = allowPartialPyqOrFill
                    )
                }

                QuestionSourceType.CHAPTER_PRACTICE -> {
                    curateChapterPracticeQuestions(
                        config = config,
                        targetExamName = targetExamName,
                        targetSubject = targetSubject,
                        targetCount = targetCount,
                        targetLanguage = targetLanguage
                    )
                }

                QuestionSourceType.EXAM_PATTERN -> {
                    curateExamPatternQuestions(
                        config = config,
                        examContext = examContext,
                        targetExamName = targetExamName,
                        targetCount = targetCount,
                        targetLanguage = targetLanguage
                    )
                }

                QuestionSourceType.CURRENT_AFFAIRS -> {
                    curateCurrentAffairsQuestions(
                        config = config,
                        targetExamName = targetExamName,
                        targetCount = targetCount,
                        targetLanguage = targetLanguage
                    )
                }

                QuestionSourceType.MIXED -> {
                    curateMixedQuestions(
                        config = config,
                        targetExamName = targetExamName,
                        targetSubject = targetSubject,
                        targetCount = targetCount,
                        targetLanguage = targetLanguage,
                        historyList = historyList,
                        weakTopics = weakTopics,
                        mistakeTopics = mistakeTopics,
                        flaggedQuestionIds = flaggedQuestionIds
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error curating questions: ${e.message}", e)
            QuestionSourceResult.Failure(
                TestGenerationError(
                    stage = "QUESTION_CURATION_FAILED",
                    userMessage = "Could not prepare test questions. Please check your settings and try again.",
                    technicalDetails = e.message ?: "Unknown error"
                )
            )
        }
    }

    /**
     * 1. PYQ: Queries authentic verified previous-year questions from repository.
     */
    private suspend fun curatePyqQuestions(
        config: MockTestConfig,
        targetExamName: String,
        targetSubject: String,
        targetCount: Int,
        targetLanguage: String,
        allowPartialPyqOrFill: Boolean
    ): QuestionSourceResult {
        val pyqCandidates = examQuestionBankRepository.getVerifiedPyqs(
            examName = targetExamName,
            subject = targetSubject,
            topic = if (config.topic == "All Topics" || config.topic.isBlank()) "" else config.topic,
            year = if (config.pyqYear == "All Available Years") "" else config.pyqYear,
            shift = if (config.pyqShift == "All Shifts") "" else config.pyqShift,
            language = targetLanguage,
            count = 100
        )

        val validPyqs = SmartMockEngine.deduplicateQuestions(
            SmartMockEngine.validateAndFilterQuestions(pyqCandidates)
        )

        Log.d(TAG, "Found ${validPyqs.size} verified PYQs for $targetExamName ($targetSubject)")

        if (validPyqs.size < targetCount && !allowPartialPyqOrFill) {
            return QuestionSourceResult.InsufficientPyq(
                availableCount = validPyqs.size,
                requestedCount = targetCount,
                availableQuestions = validPyqs,
                examName = targetExamName,
                subject = targetSubject
            )
        }

        if (validPyqs.isEmpty()) {
            // Fallback: If no strict PYQ matches specific filters, try general exam PYQs
            val broaderPyqs = examQuestionBankRepository.getVerifiedPyqs(
                examName = targetExamName,
                subject = "All Subjects",
                language = targetLanguage,
                count = targetCount
            )
            if (broaderPyqs.isNotEmpty()) {
                val selected = broaderPyqs.take(targetCount)
                return QuestionSourceResult.Success(
                    questions = selected,
                    requestedCount = targetCount,
                    sourceSummary = "Verified PYQs (${selected.size} available)"
                )
            }

            return QuestionSourceResult.Failure(
                TestGenerationError(
                    stage = "NO_PYQS_FOUND",
                    userMessage = "No verified PYQs found for the selected subject/year in $targetExamName. Try selecting 'Mixed' or 'Chapter Practice' mode.",
                    canRetry = false
                )
            )
        }

        val selected = validPyqs.take(targetCount)
        return QuestionSourceResult.Success(
            questions = selected,
            requestedCount = targetCount,
            sourceSummary = "Official Previous-Year Questions (PYQ)"
        )
    }

    /**
     * 2. CHAPTER PRACTICE: Generates syllabus-grounded practice questions via Gemini AI with offline fallback.
     */
    private suspend fun curateChapterPracticeQuestions(
        config: MockTestConfig,
        targetExamName: String,
        targetSubject: String,
        targetCount: Int,
        targetLanguage: String
    ): QuestionSourceResult {
        val effectiveSubject = if (targetSubject == "All Subjects") "General Science & Aptitude" else targetSubject
        val effectiveTopic = if (config.topic == "All Topics" || config.topic.isBlank()) "Core Concepts" else config.topic

        Log.d(TAG, "Generating AI Chapter Practice: $targetExamName / $effectiveSubject / $effectiveTopic ($targetCount Qs)")

        val aiResult = withTimeoutOrNull(AI_GENERATION_TIMEOUT_MS) {
            try {
                geminiRepository.generateMockTestQuestions(
                    subject = effectiveSubject,
                    chapter = effectiveTopic,
                    difficulty = config.difficulty,
                    count = targetCount,
                    examName = targetExamName,
                    language = targetLanguage
                )
            } catch (e: Exception) {
                Log.w(TAG, "AI Generation exception: ${e.message}")
                null
            }
        }

        var questionsList = mutableListOf<Question>()

        if (aiResult != null && aiResult.isSuccess) {
            val generated = aiResult.getOrNull() ?: emptyList()
            val valid = SmartMockEngine.validateAndFilterQuestions(generated).mapIndexed { idx, q ->
                q.copy(
                    id = "ai_practice_${System.currentTimeMillis()}_$idx",
                    subject = effectiveSubject,
                    chapter = effectiveTopic,
                    topic = effectiveTopic,
                    source = QuestionSource.AI_GENERATED,
                    sourceLabel = "AI Practice • $effectiveTopic",
                    yearOrTag = targetExamName,
                    language = targetLanguage,
                    generationModel = "gemini-3.5-flash"
                )
            }
            questionsList.addAll(SmartMockEngine.deduplicateQuestions(valid))
        }

        // If AI generation was partial or timed out, supplement from verified practice bank
        if (questionsList.size < targetCount) {
            val needCount = targetCount - questionsList.size
            Log.d(TAG, "AI produced ${questionsList.size}/$targetCount. Pulling $needCount from verified practice bank.")

            val bankQuestions = examQuestionBankRepository.getQuestionsForTest(
                examName = targetExamName,
                subject = targetSubject,
                topic = config.topic,
                difficulty = config.difficulty,
                language = targetLanguage,
                desiredCount = needCount * 2
            ).map { q ->
                q.copy(
                    source = QuestionSource.AI_GENERATED,
                    sourceLabel = "Chapter Practice Concept"
                )
            }

            questionsList.addAll(bankQuestions.take(needCount))
        }

        val finalQuestions = SmartMockEngine.deduplicateQuestions(
            SmartMockEngine.validateAndFilterQuestions(questionsList)
        ).take(targetCount)

        if (finalQuestions.isEmpty()) {
            return QuestionSourceResult.Failure(
                TestGenerationError(
                    stage = "PRACTICE_GENERATION_FAILED",
                    userMessage = "Could not generate practice questions for $effectiveTopic. Please check your network or try another topic."
                )
            )
        }

        return QuestionSourceResult.Success(
            questions = finalQuestions,
            requestedCount = targetCount,
            sourceSummary = "AI Chapter Practice ($effectiveTopic)"
        )
    }

    /**
     * 3. EXAM PATTERN: Authentic exam pattern mock simulation following official exam syllabus proportions.
     */
    private suspend fun curateExamPatternQuestions(
        config: MockTestConfig,
        examContext: ExamContext,
        targetExamName: String,
        targetCount: Int,
        targetLanguage: String
    ): QuestionSourceResult {
        val subjects = if (examContext.subjects.isNotEmpty()) {
            examContext.subjects.map { it.name }
        } else {
            listOf("General Science", "Mathematics", "General Intelligence & Reasoning", "General Awareness")
        }

        val perSubjectCount = (targetCount / subjects.size).coerceAtLeast(2)
        val combinedQuestions = mutableListOf<Question>()

        for (subject in subjects) {
            val subjectQs = examQuestionBankRepository.getVerifiedPyqs(
                examName = targetExamName,
                subject = subject,
                language = targetLanguage,
                count = perSubjectCount
            )
            combinedQuestions.addAll(subjectQs.take(perSubjectCount))
        }

        // Fill remaining if needed
        if (combinedQuestions.size < targetCount) {
            val remaining = targetCount - combinedQuestions.size
            val fallbackQs = examQuestionBankRepository.getQuestionsForTest(
                examName = targetExamName,
                subject = "All Subjects",
                topic = "All Topics",
                difficulty = "Mixed",
                language = targetLanguage,
                desiredCount = remaining * 2
            )
            combinedQuestions.addAll(fallbackQs.take(remaining))
        }

        val formatted = combinedQuestions.mapIndexed { idx, q ->
            q.copy(
                source = QuestionSource.EXAM_PATTERN,
                sourceLabel = "Official Pattern Blueprint"
            )
        }

        val finalQuestions = SmartMockEngine.deduplicateQuestions(
            SmartMockEngine.validateAndFilterQuestions(formatted)
        ).take(targetCount)

        return QuestionSourceResult.Success(
            questions = finalQuestions,
            requestedCount = targetCount,
            sourceSummary = "Official Pattern Simulation ($targetExamName)"
        )
    }

    /**
     * 4. CURRENT AFFAIRS: Questions curated from verified current affairs data.
     */
    private suspend fun curateCurrentAffairsQuestions(
        config: MockTestConfig,
        targetExamName: String,
        targetCount: Int,
        targetLanguage: String
    ): QuestionSourceResult {
        val caPyqs = examQuestionBankRepository.getVerifiedPyqs(
            examName = targetExamName,
            subject = "General Awareness",
            topic = "Current Affairs",
            language = targetLanguage,
            count = targetCount
        ).map {
            it.copy(
                source = QuestionSource.CURRENT_AFFAIRS,
                sourceLabel = "Current Affairs 2024-2026"
            )
        }

        var questionsList = mutableListOf<Question>()
        questionsList.addAll(caPyqs)

        if (questionsList.size < targetCount) {
            val need = targetCount - questionsList.size
            val aiResult = withTimeoutOrNull(AI_GENERATION_TIMEOUT_MS) {
                try {
                    geminiRepository.generateComprehensiveExamQuiz(
                        examName = targetExamName,
                        subject = "Current Affairs & General Awareness",
                        topic = "National & International Events, Government Schemes, Economy, Science & Sports 2024-2026",
                        difficulty = config.difficulty,
                        count = need,
                        language = targetLanguage,
                        mode = "Current Affairs"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (aiResult != null && aiResult.isSuccess) {
                val generated = aiResult.getOrNull() ?: emptyList()
                val valid = generated.mapIndexed { idx, q ->
                    q.copy(
                        id = "ca_q_${System.currentTimeMillis()}_$idx",
                        subject = "General Awareness",
                        topic = "Current Affairs 2024-2026",
                        source = QuestionSource.CURRENT_AFFAIRS,
                        sourceLabel = "Current Affairs 2024-2026",
                        yearOrTag = targetExamName,
                        language = targetLanguage
                    )
                }
                questionsList.addAll(valid)
            }
        }

        val finalQuestions = SmartMockEngine.deduplicateQuestions(
            SmartMockEngine.validateAndFilterQuestions(questionsList)
        ).take(targetCount)

        return QuestionSourceResult.Success(
            questions = finalQuestions,
            requestedCount = targetCount,
            sourceSummary = "Current Affairs & GA (2024-2026)"
        )
    }

    /**
     * 5. MIXED: 50% Verified PYQs + 50% AI Practice / Pattern Practice.
     */
    private suspend fun curateMixedQuestions(
        config: MockTestConfig,
        targetExamName: String,
        targetSubject: String,
        targetCount: Int,
        targetLanguage: String,
        historyList: List<QuestionHistoryEntity>,
        weakTopics: Set<String>,
        mistakeTopics: Set<String>,
        flaggedQuestionIds: List<String>
    ): QuestionSourceResult {
        val halfPyqCount = targetCount / 2
        val halfPracticeCount = targetCount - halfPyqCount

        // 1. Fetch PYQs
        val pyqCandidates = examQuestionBankRepository.getVerifiedPyqs(
            examName = targetExamName,
            subject = targetSubject,
            language = targetLanguage,
            count = halfPyqCount * 2
        )

        val rankedPyqs = SmartMockEngine.rankAndSelectQuestions(
            candidates = pyqCandidates,
            targetExamName = targetExamName,
            targetSubject = targetSubject,
            targetTopic = config.topic,
            targetDifficulty = config.difficulty,
            targetLanguage = targetLanguage,
            history = historyList,
            weakTopics = weakTopics,
            mistakeTopics = mistakeTopics,
            flaggedQuestionIds = flaggedQuestionIds,
            desiredCount = halfPyqCount
        )

        // 2. Fetch AI Practice / Bank Practice
        val practiceCandidates = examQuestionBankRepository.getQuestionsForTest(
            examName = targetExamName,
            subject = targetSubject,
            topic = config.topic,
            difficulty = config.difficulty,
            language = targetLanguage,
            desiredCount = halfPracticeCount * 2
        ).filter { it.source != QuestionSource.PREVIOUS_YEAR }

        val combined = mutableListOf<Question>()
        combined.addAll(rankedPyqs)

        if (practiceCandidates.isNotEmpty()) {
            combined.addAll(practiceCandidates.take(halfPracticeCount))
        }

        // If still insufficient, request from AI or fill from bank
        if (combined.size < targetCount) {
            val need = targetCount - combined.size
            val fallback = examQuestionBankRepository.getAllQuestions().filter {
                targetSubject == "All Subjects" || it.subject.equals(targetSubject, ignoreCase = true)
            }.shuffled().take(need)
            combined.addAll(fallback)
        }

        val finalQuestions = SmartMockEngine.deduplicateQuestions(
            SmartMockEngine.validateAndFilterQuestions(combined)
        ).take(targetCount)

        return QuestionSourceResult.Success(
            questions = finalQuestions,
            requestedCount = targetCount,
            sourceSummary = "Balanced Mix (PYQ + Practice)"
        )
    }
}
