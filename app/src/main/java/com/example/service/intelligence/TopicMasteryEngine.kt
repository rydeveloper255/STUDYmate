package com.example.service.intelligence

import com.example.data.model.*
import kotlin.math.roundToInt

/**
 * Centralized, deterministic Topic Mastery & Preparation Intelligence Engine.
 * Formulates user topic mastery, subject/chapter aggregations, exam readiness score,
 * and time-budgeted study recommendations based purely on real user activity data.
 */
object TopicMasteryEngine {

    object Config {
        const val LOW_CONFIDENCE_THRESHOLD = 5
        const val HIGH_CONFIDENCE_THRESHOLD = 15
        const val WEAK_SCORE_THRESHOLD = 50
        const val STRONG_SCORE_THRESHOLD = 70
        const val MASTERED_SCORE_THRESHOLD = 85
        const val REVISION_DUE_DAYS_MILLIS = 14L * 24 * 60 * 60 * 1000
    }

    /**
     * Determines the confidence level based on total questions attempted.
     */
    fun calculateConfidenceLevel(totalAttempts: Int): String {
        return when {
            totalAttempts >= Config.HIGH_CONFIDENCE_THRESHOLD -> "HIGH"
            totalAttempts >= Config.LOW_CONFIDENCE_THRESHOLD -> "MEDIUM"
            else -> "LOW"
        }
    }

    /**
     * Computes accuracy percentage safely without division errors.
     */
    fun safeAccuracyPercent(correct: Int, attempted: Int): Float {
        if (attempted <= 0) return 0f
        return (correct.toFloat() / attempted) * 100f
    }

    /**
     * Updates and recalculates TopicMastery entity fields deterministically.
     */
    fun computeUpdatedMastery(
        existing: TopicMastery?,
        examId: String,
        subjectId: String,
        chapterId: String,
        topicId: String,
        subjectName: String,
        chapterName: String,
        topicName: String,
        studyMinutesDelta: Int = 0,
        studyCompleted: Boolean = false,
        practiceAttemptedDelta: Int = 0,
        practiceCorrectDelta: Int = 0,
        mockAttemptedDelta: Int = 0,
        mockCorrectDelta: Int = 0,
        newMistakeCountDelta: Int = 0,
        manualOverride: String? = null
    ): TopicMastery {
        val now = System.currentTimeMillis()

        val totalStudyMinutes = (existing?.totalStudyMinutes ?: 0) + studyMinutesDelta
        val studyStatus = when {
            studyCompleted -> "COMPLETED"
            totalStudyMinutes > 0 -> "STARTED"
            else -> existing?.studyCompletionStatus ?: "NOT_STARTED"
        }

        val practiceAttempts = (existing?.practiceAttempts ?: 0) + practiceAttemptedDelta
        val practiceCorrect = (existing?.practiceCorrect ?: 0) + practiceCorrectDelta
        val practiceAcc = safeAccuracyPercent(practiceCorrect, practiceAttempts)

        val mockAttempts = (existing?.mockAttempts ?: 0) + mockAttemptedDelta
        val mockCorrect = (existing?.mockCorrect ?: 0) + mockCorrectDelta
        val mockAcc = safeAccuracyPercent(mockCorrect, mockAttempts)

        val totalAttempts = (existing?.totalQuestionsAttempted ?: 0) + practiceAttemptedDelta + mockAttemptedDelta
        val totalCorrect = (existing?.correctQuestionsCount ?: 0) + practiceCorrectDelta + mockCorrectDelta
        val totalIncorrect = totalAttempts - totalCorrect
        val overallAcc = safeAccuracyPercent(totalCorrect, totalAttempts)

        // Recent window calculation (last 10-15 questions weight)
        val recentAttempts = ((existing?.recentAttemptsCount ?: 0) + practiceAttemptedDelta + mockAttemptedDelta).coerceAtMost(20)
        val recentCorrect = ((existing?.recentCorrectCount ?: 0) + practiceCorrectDelta + mockCorrectDelta).coerceAtMost(recentAttempts)
        val recentAcc = if (recentAttempts > 0) safeAccuracyPercent(recentCorrect, recentAttempts) else overallAcc

        val repeatedMistakes = ((existing?.repeatedMistakesCount ?: 0) + newMistakeCountDelta).coerceAtLeast(0)
        val confidence = calculateConfidenceLevel(totalAttempts)
        val override = manualOverride ?: existing?.userManualOverride ?: "NONE"

        // Master Score (0-100) Calculation
        val calculatedScore: Int = if (totalAttempts <= 0) {
            when {
                studyStatus == "COMPLETED" || totalStudyMinutes >= 30 -> 35
                totalStudyMinutes > 0 -> 20
                else -> 0
            }
        } else {
            val weightedAcc = (recentAcc * 0.35f) + (overallAcc * 0.25f) + (practiceAcc * 0.20f) + (mockAcc * 0.20f)
            val volumeFactor = (totalAttempts / 15f).coerceIn(0.25f, 1.0f)
            val studyBonus = (totalStudyMinutes / 10f).coerceAtMost(10f)
            val mistakePenalty = (repeatedMistakes * 6f).coerceAtMost(25f)

            val rawScore = (weightedAcc * volumeFactor) + studyBonus - mistakePenalty
            rawScore.roundToInt().coerceIn(0, 100)
        }

        // Determine Mastery State
        val state = evaluateMasteryState(
            score = calculatedScore,
            totalAttempts = totalAttempts,
            totalStudyMinutes = totalStudyMinutes,
            repeatedMistakes = repeatedMistakes,
            recentAcc = recentAcc,
            overallAcc = overallAcc,
            confidence = confidence,
            lastTested = existing?.lastTestedMillis ?: 0L,
            now = now,
            manualOverride = override
        )

        val lastStudied = if (studyMinutesDelta > 0 || studyCompleted) now else (existing?.lastStudiedMillis ?: 0L)
        val lastTested = if (practiceAttemptedDelta > 0 || mockAttemptedDelta > 0) now else (existing?.lastTestedMillis ?: 0L)

        return TopicMastery(
            id = existing?.id ?: 0,
            userId = existing?.userId ?: "current_user",
            examId = examId,
            subjectId = subjectId,
            chapterId = chapterId,
            topicId = topicId,
            subject = subjectName,
            chapter = chapterName,
            topic = topicName,
            studyCompletionStatus = studyStatus,
            totalStudyMinutes = totalStudyMinutes,
            practiceAttempts = practiceAttempts,
            practiceCorrect = practiceCorrect,
            practiceAccuracyPercent = practiceAcc,
            mockAttempts = mockAttempts,
            mockCorrect = mockCorrect,
            recentAttemptsCount = recentAttempts,
            recentCorrectCount = recentCorrect,
            recentAccuracyPercent = recentAcc,
            repeatedMistakesCount = repeatedMistakes,
            confidenceLevel = confidence,
            masteryState = state,
            masteryScore = calculatedScore,
            accuracyPercent = overallAcc,
            totalQuestionsAttempted = totalAttempts,
            correctQuestionsCount = totalCorrect,
            incorrectQuestionsCount = totalIncorrect,
            userManualOverride = override,
            lastStudiedMillis = lastStudied,
            lastTestedMillis = lastTested,
            updatedAt = now
        )
    }

    private fun evaluateMasteryState(
        score: Int,
        totalAttempts: Int,
        totalStudyMinutes: Int,
        repeatedMistakes: Int,
        recentAcc: Float,
        overallAcc: Float,
        confidence: String,
        lastTested: Long,
        now: Long,
        manualOverride: String
    ): String {
        // Handle manual override preferences
        if (manualOverride == "NEED_HELP") return "WEAK"
        if (manualOverride == "I_KNOW_THIS") return if (score >= 80) "MASTERED" else "STRONG"

        if (totalAttempts <= 0) {
            return if (totalStudyMinutes > 0) "LEARNING" else "NOT_STARTED"
        }

        if (repeatedMistakes >= 2 || (totalAttempts >= 3 && score < Config.WEAK_SCORE_THRESHOLD)) {
            return "WEAK"
        }

        if (recentAcc >= overallAcc + 12f && recentAcc >= 60f) {
            return "IMPROVING"
        }

        if (lastTested > 0 && (now - lastTested) > Config.REVISION_DUE_DAYS_MILLIS && score >= Config.WEAK_SCORE_THRESHOLD) {
            return "REVISION_DUE"
        }

        if (score >= Config.MASTERED_SCORE_THRESHOLD && confidence == "HIGH" && repeatedMistakes == 0) {
            return "MASTERED"
        }

        if (score >= Config.STRONG_SCORE_THRESHOLD) {
            return "STRONG"
        }

        return "PRACTICING"
    }

    /**
     * Aggregates subject-level progress for all subjects in an ExamContext.
     */
    fun buildSubjectSummaries(
        examContext: ExamContext,
        topicMasteries: List<TopicMastery>
    ): List<SubjectProgressSummary> {
        val masteryMap = topicMasteries.associateBy { it.topicId.ifBlank { "${it.subject.lowercase()}_${it.topic.lowercase()}" } }

        return examContext.subjects.map { subject ->
            val subTopics = examContext.topics.filter { it.subjectId == subject.id || it.examId == subject.examId }
            val totalTopics = if (subTopics.isNotEmpty()) subTopics.size else subject.totalTopicsCount.coerceAtLeast(1)

            val matchedMasteries = subTopics.mapNotNull { top ->
                masteryMap[top.id] ?: masteryMap["${subject.name.lowercase()}_${top.name.lowercase()}"]
            }.ifEmpty {
                topicMasteries.filter { it.subject.equals(subject.name, ignoreCase = true) }
            }

            val completedCount = matchedMasteries.count { it.studyCompletionStatus == "COMPLETED" || it.totalQuestionsAttempted > 0 }
            val masteredCount = matchedMasteries.count { it.masteryState == "MASTERED" || it.masteryScore >= Config.MASTERED_SCORE_THRESHOLD }
            val weakCount = matchedMasteries.count { it.masteryState == "WEAK" || (it.totalQuestionsAttempted > 0 && it.masteryScore < Config.WEAK_SCORE_THRESHOLD) }
            val revisionDueCount = matchedMasteries.count { it.masteryState == "REVISION_DUE" }

            val totalCorrect = matchedMasteries.sumOf { it.correctQuestionsCount }
            val totalAttempted = matchedMasteries.sumOf { it.totalQuestionsAttempted }
            val overallAcc = safeAccuracyPercent(totalCorrect, totalAttempted)

            val avgMastery = if (matchedMasteries.isNotEmpty()) {
                matchedMasteries.map { it.masteryScore }.average().roundToInt()
            } else 0

            val weakList = matchedMasteries.filter { it.masteryState == "WEAK" || it.masteryScore < Config.WEAK_SCORE_THRESHOLD }

            SubjectProgressSummary(
                subjectId = subject.id,
                subjectName = subject.name,
                totalTopicsCount = totalTopics,
                completedTopicsCount = completedCount,
                masteredTopicsCount = masteredCount,
                weakTopicsCount = weakCount,
                revisionDueCount = revisionDueCount,
                overallAccuracyPercent = overallAcc,
                averageMasteryScore = avgMastery,
                weakTopicsList = weakList
            )
        }
    }

    /**
     * Calculates overall deterministic Exam Readiness Index and component breakdown.
     */
    fun calculateExamReadiness(
        examContext: ExamContext,
        topicMasteries: List<TopicMastery>,
        recentMockAttempts: List<MockTestAttempt>,
        studyStreakDays: Int = 1,
        examDateMillis: Long = 0L
    ): ExamReadinessScore {
        val totalTopics = examContext.topics.size.coerceAtLeast(1)
        val activeMasteries = topicMasteries.filter { it.examId == examContext.examId || (it.examId.isBlank() && examContext.examId.isBlank()) }

        val masteredCount = activeMasteries.count { it.masteryState == "MASTERED" || it.masteryScore >= Config.MASTERED_SCORE_THRESHOLD }
        val weakCount = activeMasteries.count { it.masteryState == "WEAK" || (it.totalQuestionsAttempted > 0 && it.masteryScore < Config.WEAK_SCORE_THRESHOLD) }
        val revisionDueCount = activeMasteries.count { it.masteryState == "REVISION_DUE" }

        val activeTopicsCount = activeMasteries.count { it.totalQuestionsAttempted > 0 || it.totalStudyMinutes > 0 }
        val syllabusCoverage = if (totalTopics > 0) ((activeTopicsCount.toFloat() / totalTopics) * 100f).roundToInt().coerceIn(0, 100) else 0

        val subjectSummaries = buildSubjectSummaries(examContext, activeMasteries)
        val avgSubjectMastery = if (subjectSummaries.isNotEmpty()) {
            subjectSummaries.map { it.averageMasteryScore }.average().roundToInt()
        } else 0

        val minSubjectScore = subjectSummaries.minOfOrNull { it.averageMasteryScore } ?: 0
        val maxSubjectScore = (subjectSummaries.maxOfOrNull { it.averageMasteryScore } ?: 100).coerceAtLeast(1)
        val subjectBalanceScore = ((minSubjectScore.toFloat() / maxSubjectScore) * 100f).roundToInt()

        val hasMocks = recentMockAttempts.isNotEmpty()
        val recentMockAcc = if (hasMocks) {
            recentMockAttempts.take(5).map { it.accuracyPercent }.average().toFloat()
        } else 0f

        val mockPerformancePercent = recentMockAcc.roundToInt()
        val revisionHealthPercent = (100 - (revisionDueCount * 12)).coerceIn(20, 100)
        val consistencyPercent = (studyStreakDays * 15 + 25).coerceIn(25, 100)

        val daysRemaining = if (examDateMillis > System.currentTimeMillis()) {
            ((examDateMillis - System.currentTimeMillis()) / (1000L * 3600 * 24)).toInt()
        } else 0
        val isNearExamMode = daysRemaining in 1..30

        // Weighted Readiness Formula (0-100)
        val effectiveMockComponent = if (hasMocks) mockPerformancePercent else avgSubjectMastery
        val readinessScore = if (activeTopicsCount == 0 && !hasMocks) {
            0
        } else {
            (
                (syllabusCoverage * 0.25f) +
                (avgSubjectMastery * 0.30f) +
                (effectiveMockComponent * 0.20f) +
                (revisionHealthPercent * 0.15f) +
                (consistencyPercent * 0.10f)
            ).roundToInt().coerceIn(0, 100)
        }

        val weakestSubject = subjectSummaries.minByOrNull { it.averageMasteryScore }?.subjectName ?: "Core Subjects"
        val strongestSubject = subjectSummaries.maxByOrNull { it.averageMasteryScore }?.subjectName ?: "Strong Areas"

        val (status, badgeText, insight) = when {
            activeTopicsCount == 0 && !hasMocks -> Triple(
                "INSUFFICIENT_DATA",
                "Insufficient Data 🌱",
                "Begin your first study session or topic test to build your exam readiness index."
            )
            weakCount >= 3 || revisionDueCount >= 5 -> Triple(
                "NEEDS_ATTENTION",
                "Needs Attention ⚠️",
                "Multiple weak topics or pending revisions detected in $weakestSubject. Focus on targeted revision."
            )
            readinessScore >= 75 -> Triple(
                "HIGH_READINESS",
                "High Readiness 🎯",
                "Strong syllabus coverage and high topic accuracy. Focus on timed mock test speed and exam stamina."
            )
            readinessScore >= 55 -> Triple(
                "ON_TRACK",
                "On Track 🚀",
                "Solid progress! Syllabi coverage is good with steady performance in $strongestSubject."
            )
            readinessScore >= 30 -> Triple(
                "BUILDING",
                "Building Mastery ⚡",
                "Mastery is developing. Prioritize practice sets in $weakestSubject to boost scores."
            )
            else -> Triple(
                "EARLY_PREPARATION",
                "Early Preparation 🌱",
                "You are in early preparation. Complete foundational chapters to establish momentum."
            )
        }

        // Factual Explanation based on real metrics
        val explanation = when (status) {
            "INSUFFICIENT_DATA" -> "Start studying topics or take a mock test to calculate your personalized preparation metrics."
            "HIGH_READINESS" -> "Your syllabus coverage is at $syllabusCoverage% with high mastery in $strongestSubject ($avgSubjectMastery%). Keep maintaining test accuracy."
            "ON_TRACK" -> "Syllabus coverage is $syllabusCoverage% and average mastery is $avgSubjectMastery%. $weakestSubject requires slight reinforcement."
            "NEEDS_ATTENTION" -> "Syllabus coverage is $syllabusCoverage%, but $weakCount weak topics and $revisionDueCount pending revisions require attention."
            "BUILDING" -> "You have covered $syllabusCoverage% of the syllabus. Strengthening $weakestSubject will accelerate your readiness score."
            else -> "Early phase: $syllabusCoverage% coverage achieved. Continue daily study sessions to raise mastery levels."
        }

        // Warnings list (max 3)
        val warningsList = mutableListOf<String>()
        if (weakCount > 0) {
            warningsList.add("$weakCount high-priority topics need immediate practice in $weakestSubject.")
        }
        if (revisionDueCount > 0) {
            warningsList.add("Revision backlog contains $revisionDueCount topics ready for review.")
        }
        if (!hasMocks) {
            warningsList.add("Mock test performance has not been measured yet. Take a quick diagnostic mock.")
        } else if (recentMockAcc < 50f) {
            warningsList.add("Recent mock accuracy is ${recentMockAcc.roundToInt()}%. Focus on error analysis.")
        }
        if (isNearExamMode) {
            warningsList.add("Exam is $daysRemaining days away — Near-Exam Revision Mode active.")
        }

        // Action Plan (3 concrete steps)
        val actionPlanList = listOf(
            "1. Revise weak topics in $weakestSubject (20 min focused session)",
            "2. Complete 15 practice questions on high-yield syllabus topics",
            if (hasMocks) "3. Review mistake logs from recent mock tests" else "3. Take a 30-minute subject-level mock test"
        )

        return ExamReadinessScore(
            examId = examContext.examId,
            examName = examContext.examName,
            readinessScore = readinessScore,
            status = status,
            statusBadgeText = badgeText,
            syllabusCoveragePercent = syllabusCoverage,
            topicMasteryPercent = avgSubjectMastery,
            mockPerformancePercent = mockPerformancePercent,
            revisionHealthPercent = revisionHealthPercent,
            consistencyPercent = consistencyPercent,
            subjectBalanceScore = subjectBalanceScore,
            recentAccuracyPercent = recentMockAcc,
            totalTopicsCount = totalTopics,
            masteredTopicsCount = masteredCount,
            weakTopicsCount = weakCount,
            revisionDueCount = revisionDueCount,
            actionableInsight = insight,
            explanation = explanation,
            warnings = warningsList,
            actionPlan = actionPlanList,
            isNearExamMode = isNearExamMode
        )
    }

    /**
     * Ranks candidate topics to answer "What Should I Study Next?".
     */
    fun generateRecommendations(
        examContext: ExamContext,
        topicMasteries: List<TopicMastery>,
        mistakes: List<MistakeItem>
    ): List<StudyRecommendation> {
        val masteryMap = topicMasteries.associateBy { it.topicId.ifBlank { "${it.subject.lowercase()}_${it.topic.lowercase()}" } }
        val unmasteredMistakesByTopic = mistakes.filter { !it.isMastered }.groupBy { "${it.subject.lowercase()}_${it.topic.lowercase()}" }

        val candidateList = mutableListOf<StudyRecommendation>()

        val chaptersMap = examContext.chapters.associateBy { it.id }
        val subjectsMap = examContext.subjects.associateBy { it.id }

        for (topic in examContext.topics) {
            val subject = subjectsMap[topic.subjectId]
            val chapter = chaptersMap[topic.chapterId]
            val subjectName = subject?.name ?: "General"
            val chapterName = chapter?.name ?: "General"

            val key = topic.id.ifBlank { "${subjectName.lowercase()}_${topic.name.lowercase()}" }
            val mastery = masteryMap[key] ?: topicMasteries.find { it.topic.equals(topic.name, ignoreCase = true) }
            val topicMistakes = unmasteredMistakesByTopic[key] ?: emptyList()

            val currentScore = mastery?.masteryScore ?: 0
            val state = mastery?.masteryState ?: "NOT_STARTED"
            val override = mastery?.userManualOverride ?: "NONE"

            if (override == "SKIP_FOR_NOW") continue

            var priority = (100 - currentScore)

            if (state == "WEAK") priority += 45
            if (state == "REVISION_DUE") priority += 35
            if (topic.isHighYield) priority += 25
            if (topicMistakes.isNotEmpty()) priority += (topicMistakes.size * 10).coerceAtMost(30)
            if (override == "NEED_HELP") priority += 25

            val (action, reason, duration) = when {
                state == "REVISION_DUE" -> Triple("REVISE", "Spaced Recall Due for ${topic.name}", 20)
                state == "WEAK" || topicMistakes.isNotEmpty() -> Triple("MISTAKE_REVIEW", "High mistakes in ${topic.name}", 25)
                state == "NOT_STARTED" -> Triple("LEARN_NEW", "Core topic in $subjectName", 30)
                else -> Triple("PRACTICE", "Targeted practice for ${topic.name}", 25)
            }

            candidateList.add(
                StudyRecommendation(
                    topicId = topic.id,
                    examId = examContext.examId,
                    subjectName = subjectName,
                    chapterName = chapterName,
                    topicName = topic.name,
                    recommendedAction = action,
                    reason = reason,
                    priorityScore = priority,
                    recommendedDurationMinutes = duration,
                    masteryState = state,
                    currentMasteryScore = currentScore,
                    isHighYield = topic.isHighYield
                )
            )
        }

        return candidateList.sortedByDescending { it.priorityScore }.take(10)
    }

    /**
     * Generates a time-budgeted daily study recommendation.
     */
    fun generateDailyStudyPlan(
        availableMinutes: Int,
        recommendations: List<StudyRecommendation>,
        targetExamName: String
    ): DailyStudyPlan {
        val budget = availableMinutes.coerceIn(15, 300)
        var remaining = budget
        val selectedItems = mutableListOf<StudyRecommendation>()

        for (rec in recommendations) {
            if (remaining <= 0) break
            val duration = rec.recommendedDurationMinutes.coerceAtMost(remaining)
            if (duration >= 10) {
                selectedItems.add(rec.copy(recommendedDurationMinutes = duration))
                remaining -= duration
            }
        }

        val advice = if (selectedItems.isNotEmpty()) {
            "Today's $budget-min target: Focus on ${selectedItems.first().subjectName} (${selectedItems.first().topicName})."
        } else {
            "Complete your scheduled study goals for $targetExamName."
        }

        return DailyStudyPlan(
            totalAvailableMinutes = budget,
            targetExamName = targetExamName,
            items = selectedItems,
            summaryAdvice = advice
        )
    }
}
