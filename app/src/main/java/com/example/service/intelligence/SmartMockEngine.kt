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
        val formattedPenalty = String.format(java.util.Locale.US, "%.2f", negativePenalty)
        val schemeLabel = if (negativePenalty > 0) "+${marksPerCorrect.toInt()} / -$formattedPenalty (Negative Marking)" else "+${marksPerCorrect.toInt()} / 0 (No Negative)"

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

    /**
     * Calculates time analysis metrics: total time, average per question, fastest and longest questions.
     */
    fun calculateTimeAnalysis(
        details: List<QuestionAttemptDetail>,
        totalAllowedSeconds: Int,
        remainingSeconds: Int
    ): TimeAnalysisResult {
        val totalTimeSpent = (totalAllowedSeconds - remainingSeconds).coerceAtLeast(0)
        val totalQuestions = details.size
        val avgTime = if (totalQuestions > 0) totalTimeSpent.toFloat() / totalQuestions else 0f

        var fastestIdx = 0
        var fastestSecs = Int.MAX_VALUE
        var longestIdx = 0
        var longestSecs = 0

        details.forEachIndexed { idx, detail ->
            val spent = detail.timeSpentSeconds
            if (spent in 1 until fastestSecs) {
                fastestSecs = spent
                fastestIdx = idx
            }
            if (spent > longestSecs) {
                longestSecs = spent
                longestIdx = idx
            }
        }

        if (fastestSecs == Int.MAX_VALUE) fastestSecs = (avgTime.toInt()).coerceAtLeast(1)
        if (longestSecs == 0) longestSecs = (avgTime.toInt()).coerceAtLeast(1)

        return TimeAnalysisResult(
            totalTimeSpentSeconds = totalTimeSpent,
            avgTimePerQuestionSeconds = avgTime,
            fastestQuestionIndex = fastestIdx,
            fastestTimeSeconds = fastestSecs,
            longestQuestionIndex = longestIdx,
            longestTimeSeconds = longestSecs
        )
    }

    /**
     * Computes complete, multi-dimensional test intelligence result.
     */
    fun computeTestIntelligence(
        attempt: MockTestAttempt,
        details: List<QuestionAttemptDetail>,
        testType: MockTestType = MockTestType.FULL_MOCK,
        language: String = "English"
    ): TestIntelligenceResult {
        val totalQuestions = details.size.coerceAtLeast(attempt.totalQuestions)
        val attempted = details.count { it.selectedIndex != null }
        val correctCount = details.count { it.isCorrect }
        val incorrectCount = attempted - correctCount
        val skippedCount = totalQuestions - attempted
        val timeSpent = attempt.timeSpentSeconds
        val avgTime = if (totalQuestions > 0) timeSpent.toFloat() / totalQuestions else 0f
        val accuracy = if (attempted > 0) (correctCount.toFloat() / attempted) * 100f else 0f

        // 1. Performance Category
        val category = when {
            accuracy >= 88f -> PerformanceCategory("Excellent", "Outstanding mastery across tested concepts!", "🏆")
            accuracy >= 78f -> PerformanceCategory("Strong", "Solid performance! Minor targeted practice will unlock peak scores.", "🌟")
            accuracy >= 65f -> PerformanceCategory("Good", "Good understanding! Focus on weak topics to boost consistency.", "👍")
            accuracy >= 50f -> PerformanceCategory("Improving", "Promising progress! Clear solutions step-by-step to build speed.", "📈")
            else -> PerformanceCategory("Needs Practice", "Great starting baseline. Focus on core concepts to build confidence.", "📚")
        }

        // 2. Subject-wise Analysis
        val subjectGroups = details.groupBy { it.question.subject.ifBlank { attempt.subject }.ifBlank { "General" } }
        val subjectPerformances = subjectGroups.map { (subName, qList) ->
            val subAtt = qList.count { it.selectedIndex != null }
            val subCorr = qList.count { it.isCorrect }
            val subIncorr = subAtt - subCorr
            val subUnans = qList.size - subAtt
            val subAcc = if (subAtt > 0) (subCorr.toFloat() / subAtt) * 100f else 0f
            val subTime = qList.sumOf { it.timeSpentSeconds }
            val subAvgTime = if (qList.isNotEmpty()) subTime.toFloat() / qList.size else 0f
            val subScore = (subCorr * 4f) - (subIncorr * 1f)

            SubjectPerformance(
                subject = subName,
                totalQuestions = qList.size,
                attempted = subAtt,
                correct = subCorr,
                incorrect = subIncorr,
                unanswered = subUnans,
                accuracyPercent = subAcc,
                score = subScore,
                timeSpentSeconds = subTime,
                avgTimePerQuestionSeconds = subAvgTime
            )
        }

        // 3. Chapter / Topic-wise Analysis
        val chapterGroups = details.groupBy {
            val q = it.question
            if (q.chapter.isNotBlank()) q.chapter else if (q.topic.isNotBlank()) q.topic else "General Concepts"
        }
        val chapterPerformances = chapterGroups.map { (chapName, qList) ->
            val chapSub = qList.firstOrNull()?.question?.subject ?: attempt.subject
            val chapTopic = qList.firstOrNull()?.question?.topic ?: chapName
            val chapAtt = qList.count { it.selectedIndex != null }
            val chapCorr = qList.count { it.isCorrect }
            val chapIncorr = chapAtt - chapCorr
            val chapUnans = qList.size - chapAtt
            val chapAcc = if (chapAtt > 0) (chapCorr.toFloat() / chapAtt) * 100f else 0f
            val hasSufficientData = qList.size >= 2

            ChapterPerformance(
                chapter = chapName,
                topic = chapTopic,
                subject = chapSub,
                totalQuestions = qList.size,
                attempted = chapAtt,
                correct = chapCorr,
                incorrect = chapIncorr,
                unanswered = chapUnans,
                accuracyPercent = chapAcc,
                hasSufficientData = hasSufficientData
            )
        }

        // 4. Weak Area & Strong Area Detection
        val weakTopicsList = mutableListOf<String>()
        val strongTopicsList = mutableListOf<String>()

        chapterPerformances.forEach { cp ->
            if (cp.hasSufficientData) {
                if (cp.accuracyPercent < 65f || cp.incorrect >= 2) {
                    weakTopicsList.add(cp.chapter)
                } else if (cp.accuracyPercent >= 75f) {
                    strongTopicsList.add(cp.chapter)
                }
            } else {
                if (cp.incorrect > 0 && cp.totalQuestions < 3) {
                    // Note limited data
                    weakTopicsList.add("${cp.chapter} (Limited data — practice more)")
                }
            }
        }

        if (weakTopicsList.isEmpty() && attempt.weakTopics.isNotEmpty()) {
            weakTopicsList.addAll(attempt.weakTopics)
        }
        if (strongTopicsList.isEmpty() && attempt.strongTopics.isNotEmpty()) {
            strongTopicsList.addAll(attempt.strongTopics)
        }

        // 5. Time + Accuracy Matrix Analysis
        var highTimeWrong = 0
        var lowTimeWrong = 0
        var highTimeCorrect = 0
        var lowTimeCorrect = 0
        var fastestIdx = 0
        var fastestSecs = Int.MAX_VALUE
        var longestIdx = 0
        var longestSecs = 0

        details.forEachIndexed { idx, detail ->
            val spent = detail.timeSpentSeconds
            val isSlow = spent > (avgTime * 1.25f)
            val isFast = spent < (avgTime * 0.5f)

            if (spent in 1 until fastestSecs) {
                fastestSecs = spent
                fastestIdx = idx
            }
            if (spent > longestSecs) {
                longestSecs = spent
                longestIdx = idx
            }

            if (detail.selectedIndex != null) {
                if (!detail.isCorrect) {
                    if (isSlow) highTimeWrong++
                    if (isFast) lowTimeWrong++
                } else {
                    if (isSlow) highTimeCorrect++
                    if (isFast) lowTimeCorrect++
                }
            }
        }

        if (fastestSecs == Int.MAX_VALUE) fastestSecs = (avgTime.toInt()).coerceAtLeast(1)
        if (longestSecs == 0) longestSecs = (avgTime.toInt()).coerceAtLeast(1)

        val neutralAdvice = when {
            lowTimeWrong > 1 -> "This pattern indicates a few quick choices resulted in errors. Rechecking steps before submitting can catch simple errors."
            highTimeWrong > 1 -> "Certain complex questions required extra time without yielding correct answers. Targeted practice on foundational concepts will build solving efficiency."
            highTimeCorrect > 1 -> "You solved several questions correctly but required extra time. Regular formula practice will increase your solving pace."
            else -> "Your speed and accuracy were well balanced throughout the test questions."
        }

        val timeMatrix = TimeAccuracyPatternMatrix(
            highTimeWrongCount = highTimeWrong,
            lowTimeWrongCount = lowTimeWrong,
            highTimeCorrectCount = highTimeCorrect,
            lowTimeCorrectCount = lowTimeCorrect,
            fastestQuestionIndex = fastestIdx,
            fastestTimeSeconds = fastestSecs,
            longestQuestionIndex = longestIdx,
            longestTimeSeconds = longestSecs,
            neutralAdvice = neutralAdvice
        )

        // 6. NOVA Post-Test Insight (Deterministic Fallback)
        val isHindi = language.equals("Hindi", ignoreCase = true)
        val strongTopicsStr = strongTopicsList.take(2).joinToString(", ")
        val weakTopicsStr = weakTopicsList.take(3).joinToString(", ")

        val whatWentWell = if (isHindi) {
            listOf(
                "Aapne overall test mein ${accuracy.toInt()}% accuracy achieve ki.",
                if (strongTopicsList.isNotEmpty()) "Strong control over $strongTopicsStr." else "Pace aur attempt ratio balanced raha."
            )
        } else {
            listOf(
                "Achieved an overall accuracy rate of ${accuracy.toInt()}%.",
                if (strongTopicsList.isNotEmpty()) "Strong performance in $strongTopicsStr." else "Maintained a steady solving pace across questions."
            )
        }

        val cleanWeak = weakTopicsList.map { it.replace(" (Limited data — practice more)", "") }.distinct()
        val cleanWeakStr = cleanWeak.take(3).joinToString(", ")
        val whatNeedsPractice = if (isHindi) {
            if (cleanWeak.isNotEmpty()) listOf("$cleanWeakStr topics par targeted practice zaroori hai.") else listOf("Complex numerical problem solving mein clarity badhayein.")
        } else {
            if (cleanWeak.isNotEmpty()) listOf("Targeted practice recommended for $cleanWeakStr.") else listOf("Practice multi-step numerical calculation speed.")
        }

        val topWeakTopic = cleanWeak.firstOrNull() ?: attempt.topic.ifBlank { "Core Concepts" }
        val recommendedNext = if (isHindi) {
            "Suggested: Try a 15-question practice set on $topWeakTopic."
        } else {
            "Suggested: Try a 15-question practice set on $topWeakTopic."
        }

        val novaInsight = NovaPostTestInsight(
            whatWentWell = whatWentWell,
            whatNeedsPractice = whatNeedsPractice,
            recommendedNextStep = recommendedNext,
            language = language,
            isAiGenerated = false
        )

        // 7. Smart Recommendations (Max 3)
        val recommendationsList = mutableListOf<SmartPracticeRecommendation>()

        // Rec 1: Weak topics
        recommendationsList.add(
            SmartPracticeRecommendation(
                id = "rec_weak_1",
                title = "Practice Weak Topics",
                subtitle = "Focus set on $topWeakTopic",
                priority = 1,
                targetExam = attempt.examName,
                targetSubject = subjectPerformances.firstOrNull { it.accuracyPercent < 70f }?.subject ?: attempt.subject,
                targetChapter = topWeakTopic,
                targetTopic = topWeakTopic,
                recommendedQuestionCount = 15,
                recommendedDifficulty = attempt.difficulty,
                recommendedType = MockTestType.WEAK_AREAS
            )
        )

        // Rec 2: Retry incorrect
        if (incorrectCount > 0) {
            recommendationsList.add(
                SmartPracticeRecommendation(
                    id = "rec_retry_incorrect",
                    title = "Retry Incorrect Questions",
                    subtitle = "Re-solve $incorrectCount questions missed in this test",
                    priority = 2,
                    targetExam = attempt.examName,
                    targetSubject = attempt.subject,
                    targetChapter = topWeakTopic,
                    targetTopic = topWeakTopic,
                    recommendedQuestionCount = incorrectCount,
                    recommendedDifficulty = attempt.difficulty,
                    recommendedType = MockTestType.PREVIOUS_MISTAKES
                )
            )
        }

        // Rec 3: Skipped questions or Revision set
        if (skippedCount > 0) {
            recommendationsList.add(
                SmartPracticeRecommendation(
                    id = "rec_retry_unanswered",
                    title = "Retry Unanswered Questions",
                    subtitle = "Complete $skippedCount unattempted questions",
                    priority = 3,
                    targetExam = attempt.examName,
                    targetSubject = attempt.subject,
                    targetChapter = "Skipped",
                    targetTopic = "Skipped",
                    recommendedQuestionCount = skippedCount,
                    recommendedDifficulty = attempt.difficulty,
                    recommendedType = MockTestType.REVISION_TEST
                )
            )
        } else {
            recommendationsList.add(
                SmartPracticeRecommendation(
                    id = "rec_revision",
                    title = "Revision & Formula Recall",
                    subtitle = "Consolidate learning for ${attempt.subject}",
                    priority = 4,
                    targetExam = attempt.examName,
                    targetSubject = attempt.subject,
                    targetChapter = "Revision",
                    targetTopic = "All Topics",
                    recommendedQuestionCount = 20,
                    recommendedDifficulty = attempt.difficulty,
                    recommendedType = MockTestType.REVISION_TEST
                )
            )
        }

        return TestIntelligenceResult(
            examName = attempt.examName,
            title = attempt.title,
            testType = testType,
            score = attempt.rawScoreEarned.ifZero(attempt.score.toFloat()),
            totalQuestions = totalQuestions,
            accuracyPercent = accuracy,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            skippedCount = skippedCount,
            timeSpentSeconds = timeSpent,
            avgTimePerQuestionSeconds = avgTime,
            performanceCategory = category,
            subjectPerformances = subjectPerformances,
            chapterPerformances = chapterPerformances,
            weakTopics = cleanWeak,
            strongTopics = strongTopicsList.distinct(),
            timeMatrix = timeMatrix,
            novaInsight = novaInsight,
            recommendations = recommendationsList.take(3)
        )
    }

    private fun Float.ifZero(defaultVal: Float): Float = if (this == 0f) defaultVal else this
}

