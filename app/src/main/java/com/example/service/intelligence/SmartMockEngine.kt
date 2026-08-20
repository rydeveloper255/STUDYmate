package com.example.service.intelligence

import com.example.data.model.*
import kotlin.math.roundToInt

/**
 * Smart Adaptive Mock Test + Question Intelligence Engine.
 * Provides validation, duplicate detection, adaptive difficulty progression,
 * weak area / mistake / revision test building, and deterministic scoring.
 */
object SmartMockEngine {

    /**
     * Validates that questions pass strict structure and quality standards.
     * Rejects malformed questions.
     */
    fun validateQuestion(question: Question): Boolean {
        if (question.questionText.isBlank() || question.questionText.trim().length < 8) return false
        if (question.options.size < 2) return false
        if (question.options.any { it.isBlank() }) return false
        if (question.correctOptionIndex !in 0 until question.options.size) return false
        if (question.explanation.isBlank()) return false
        return true
    }

    /**
     * Filters out invalid questions and ensures quality.
     */
    fun validateAndFilterQuestions(questions: List<Question>): List<Question> {
        return questions.filter { validateQuestion(it) }
    }

    /**
     * Removes exact and near-duplicate questions within the same candidate list.
     */
    fun deduplicateQuestions(questions: List<Question>): List<Question> {
        val seenTexts = mutableSetOf<String>()
        val result = mutableListOf<Question>()

        for (q in questions) {
            val normalized = q.questionText.lowercase().replace(Regex("[^a-z0-9]"), "")
            if (normalized.length < 5) continue
            // Check exact or near substring match
            val isDuplicate = seenTexts.any { seen ->
                seen == normalized || (seen.length > 20 && normalized.length > 20 && (seen.contains(normalized) || normalized.contains(seen)))
            }
            if (!isDuplicate) {
                seenTexts.add(normalized)
                result.add(q)
            }
        }
        return result
    }

    /**
     * Deterministic Score Calculation based strictly on ExamContext parameters.
     * Never uses AI for score calculation!
     */
    fun calculateDeterministicScore(
        correctCount: Int,
        incorrectCount: Int,
        skippedCount: Int,
        examContext: ExamContext
    ): Triple<Float, Float, String> {
        val marksPerCorrect = if (examContext.totalQuestions > 0) (examContext.totalMarks.toFloat() / examContext.totalQuestions) else 4.0f
        val negativePenalty = when {
            examContext.negativeMarkingText.contains("1/3") || examContext.negativeMarkingText.contains("0.33") -> marksPerCorrect / 3.0f
            examContext.negativeMarkingText.contains("1/4") || examContext.negativeMarkingText.contains("0.25") -> marksPerCorrect / 4.0f
            examContext.negativeMarkingText.contains("0.5") -> 0.5f
            examContext.negativeMarkingText.contains("1 mark") -> 1.0f
            else -> 0.25f * marksPerCorrect
        }

        val earnedMarks = (correctCount * marksPerCorrect) - (incorrectCount * negativePenalty)
        val maxMarks = (correctCount + incorrectCount + skippedCount) * marksPerCorrect

        val percentage = if (maxMarks > 0) ((earnedMarks / maxMarks) * 100f).coerceIn(0f, 100f) else 0f
        val schemeLabel = if (negativePenalty > 0) "+${marksPerCorrect.toInt()} / -${"%.2f".format(negativePenalty)} (Negative Marking)" else "+${marksPerCorrect.toInt()} / 0 (No Negative)"

        return Triple(earnedMarks, percentage, schemeLabel)
    }

    /**
     * Selects targeted questions for Weak Areas based on Topic Mastery & Mistake History.
     */
    fun filterWeakAreaQuestions(
        allQuestions: List<Question>,
        topicMasteries: List<TopicMastery>,
        mistakes: List<MistakeItem>,
        targetSubject: String,
        desiredCount: Int
    ): List<Question> {
        // 1. Identify weak topic names
        val weakFromMastery = topicMasteries
            .filter { it.masteryState == "WEAK" || it.masteryScore < 60 }
            .map { it.topic }
            .toSet()

        val weakFromMistakes = mistakes
            .filter { !it.isMastered }
            .map { it.topic }
            .toSet()

        val combinedWeakTopics = (weakFromMastery + weakFromMistakes).toSet()

        var filtered = allQuestions.filter { q ->
            (targetSubject == "All Subjects" || q.subject.equals(targetSubject, ignoreCase = true)) &&
                    (combinedWeakTopics.isEmpty() || combinedWeakTopics.any { topic -> q.topic.equals(topic, ignoreCase = true) || q.questionText.contains(topic, ignoreCase = true) })
        }

        if (filtered.size < desiredCount) {
            // Fallback to subject level questions if strictly weak topics count is insufficient
            filtered = allQuestions.filter { q ->
                targetSubject == "All Subjects" || q.subject.equals(targetSubject, ignoreCase = true)
            }
        }

        return deduplicateQuestions(validateAndFilterQuestions(filtered)).take(desiredCount)
    }

    /**
     * Selects questions for Revision Test from topics due for revision.
     */
    fun filterRevisionQuestions(
        allQuestions: List<Question>,
        topicMasteries: List<TopicMastery>,
        targetSubject: String,
        desiredCount: Int
    ): List<Question> {
        val revisionDueTopics = topicMasteries
            .filter { it.masteryState == "REVISION_DUE" || System.currentTimeMillis() >= it.recommendedReviewDateMillis }
            .map { it.topic }
            .toSet()

        var filtered = allQuestions.filter { q ->
            (targetSubject == "All Subjects" || q.subject.equals(targetSubject, ignoreCase = true)) &&
                    (revisionDueTopics.isEmpty() || revisionDueTopics.any { topic -> q.topic.equals(topic, ignoreCase = true) })
        }

        if (filtered.size < desiredCount) {
            filtered = allQuestions.filter { q ->
                targetSubject == "All Subjects" || q.subject.equals(targetSubject, ignoreCase = true)
            }
        }

        return deduplicateQuestions(validateAndFilterQuestions(filtered)).take(desiredCount)
    }

    /**
     * Generates or selects questions from user's Mistake History.
     */
    fun filterPreviousMistakeQuestions(
        allQuestions: List<Question>,
        mistakes: List<MistakeItem>,
        targetSubject: String,
        desiredCount: Int
    ): List<Question> {
        val unmasteredMistakes = mistakes.filter { !it.isMastered }

        // Try to match questions in bank by mistake questionText or topic
        val mistakeTopics = unmasteredMistakes.map { it.topic }.toSet()

        val matchedFromBank = allQuestions.filter { q ->
            (targetSubject == "All Subjects" || q.subject.equals(targetSubject, ignoreCase = true)) &&
                    (mistakeTopics.any { topic -> q.topic.equals(topic, ignoreCase = true) } ||
                            unmasteredMistakes.any { m -> q.questionText.contains(m.questionText.take(20), ignoreCase = true) })
        }

        val result = mutableListOf<Question>()
        result.addAll(matchedFromBank)

        // Convert raw mistake items into Question objects if bank questions are fewer than desiredCount
        if (result.size < desiredCount) {
            unmasteredMistakes.forEachIndexed { idx, m ->
                if (result.size < desiredCount) {
                    val converted = Question(
                        id = "mistake_q_${m.id}_$idx",
                        questionText = m.questionText,
                        options = listOf(m.correctAnswer, m.studentAnswer, "None of the above", "Cannot be determined").distinct().take(4).let { opts ->
                            if (opts.size < 4) opts + listOf("Option C", "Option D").take(4 - opts.size) else opts
                        }.shuffled(),
                        correctOptionIndex = 0, // Option matches correctly after setting correct answer index below
                        explanation = m.explanation.ifBlank { "Concept revision required for ${m.topic}." },
                        subject = m.subject,
                        topic = m.topic,
                        difficulty = "Medium",
                        source = QuestionSource.PREVIOUS_YEAR,
                        sourceLabel = "Past Mistake Concept",
                        yearOrTag = "Mistake Log"
                    )
                    // Ensure correctOptionIndex matches correctAnswer text
                    val correctIdx = converted.options.indexOf(m.correctAnswer).coerceAtLeast(0)
                    result.add(converted.copy(correctOptionIndex = correctIdx))
                }
            }
        }

        return deduplicateQuestions(validateAndFilterQuestions(result)).take(desiredCount)
    }

    /**
     * Reclassifies mistake category based on response time & answer choice.
     */
    fun classifyMistakeReason(
        question: Question,
        timeSpentSecs: Int,
        totalTimeAllowedPerQSecs: Int = 90
    ): String {
        return when {
            timeSpentSecs < 10 -> "Careless" // Answered too quickly
            timeSpentSecs > (totalTimeAllowedPerQSecs * 1.5) -> "Time Pressure" // Expended too much time
            question.difficulty.equals("Hard", ignoreCase = true) -> "Conceptual"
            else -> "Conceptual"
        }
    }

    /**
     * Dynamic difficulty adjustment for Adaptive Practice mode.
     */
    fun getNextAdaptiveDifficulty(
        currentDifficulty: String,
        isLastAnswerCorrect: Boolean
    ): String {
        return when (currentDifficulty.lowercase()) {
            "easy" -> if (isLastAnswerCorrect) "Medium" else "Easy"
            "medium" -> if (isLastAnswerCorrect) "Hard" else "Easy"
            "hard" -> if (isLastAnswerCorrect) "Hard" else "Medium"
            else -> "Medium"
        }
    }

    /**
     * Advanced Selection Priority Algorithm & Anti-Repetition Engine.
     * Ranks questions based on exam match, topic relevance, difficulty fit, weakness priority,
     * revision due status, and question history freshness.
     */
    fun rankAndSelectQuestions(
        candidates: List<Question>,
        targetExamName: String,
        targetSubject: String,
        targetTopic: String,
        targetDifficulty: String,
        targetLanguage: String,
        history: List<QuestionHistoryEntity> = emptyList(),
        weakTopics: Set<String> = emptySet(),
        mistakeTopics: Set<String> = emptySet(),
        flaggedQuestionIds: List<String> = emptyList(),
        desiredCount: Int = 10,
        testType: MockTestType = MockTestType.FULL_MOCK
    ): List<Question> {
        val flaggedSet = flaggedQuestionIds.toSet()
        val historyMap = history.associateBy { it.questionId }
        val now = System.currentTimeMillis()

        val validCandidates = validateAndFilterQuestions(candidates).filter { q ->
            q.status != "DISABLED" && !flaggedSet.contains(q.id)
        }

        val ranked = validCandidates.map { q ->
            var score = 0.0

            val qExamTag = q.yearOrTag.ifBlank { q.examId }.lowercase()
            val targetExamLower = targetExamName.lowercase()
            val isExamMatch = targetExamLower.contains("railway") && qExamTag.contains("railway") ||
                    targetExamLower.contains("ssc") && qExamTag.contains("ssc") ||
                    targetExamLower.contains("jee") && qExamTag.contains("jee") ||
                    targetExamLower.contains("neet") && qExamTag.contains("neet") ||
                    targetExamLower.contains("upsc") && qExamTag.contains("upsc") ||
                    targetExamLower.contains("bank") && qExamTag.contains("bank") ||
                    qExamTag.contains(targetExamLower) || targetExamLower.contains(qExamTag)

            if (isExamMatch) score += 1000.0 else score -= 500.0

            if (targetSubject == "All Subjects" || q.subject.equals(targetSubject, ignoreCase = true)) {
                score += 300.0
            } else {
                score -= 200.0
            }

            if (targetTopic == "All Topics" || q.topic.equals(targetTopic, ignoreCase = true)) {
                score += 400.0
            } else if (q.topic.contains(targetTopic, ignoreCase = true) || targetTopic.contains(q.topic, ignoreCase = true)) {
                score += 200.0
            }

            if (targetDifficulty == "Mixed" || targetDifficulty == "Adaptive") {
                score += 100.0
            } else if (q.difficulty.equals(targetDifficulty, ignoreCase = true)) {
                score += 250.0
            }

            val qTopic = q.topic
            if (weakTopics.any { it.equals(qTopic, ignoreCase = true) }) score += 300.0
            if (mistakeTopics.any { it.equals(qTopic, ignoreCase = true) }) score += 250.0

            val h = historyMap[q.id]
            if (h == null) {
                score += 200.0
            } else {
                val daysSinceAttempt = (now - h.lastAttemptedAt) / (1000.0 * 60 * 60 * 24)
                if (testType == MockTestType.PREVIOUS_MISTAKES) {
                    if (h.lastResult == "INCORRECT") score += 500.0
                } else if (testType == MockTestType.REVISION_TEST) {
                    if (daysSinceAttempt >= 3.0) score += 300.0
                } else {
                    if (daysSinceAttempt < 3.0) {
                        score -= 400.0 / (daysSinceAttempt + 0.5)
                    }
                    if (h.lastResult == "CORRECT") {
                        score -= 100.0
                    }
                }
            }

            if (q.language.equals(targetLanguage, ignoreCase = true)) score += 150.0

            q to score
        }

        val sorted = ranked.sortedByDescending { it.second }.map { it.first }
        return deduplicateQuestions(sorted).take(desiredCount)
    }
}
