package com.example.service.intelligence

import com.example.data.model.*
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Personalization Settings & Controls (Step 36)
 */
data class PersonalizationSettings(
    val isEnabled: Boolean = true,
    val dailyQuestionGoal: Int = 30,
    val dailyStudyMinutesGoal: Int = 45,
    val weeklyTestsGoal: Int = 3,
    val studyTimeAvailableOption: String = "30 min", // "15 min", "30 min", "60 min", "90+ min"
    val caRemindersEnabled: Boolean = true,
    val revisionRemindersEnabled: Boolean = true,
    val studyRemindersEnabled: Boolean = true,
    val testRemindersEnabled: Boolean = true,
    val goalRemindersEnabled: Boolean = true
) : Serializable

/**
 * Internal Learning Profile State (Step 36)
 */
data class LearningProfile(
    val selectedExam: String,
    val selectedLanguage: String,
    val subjects: List<String>,
    val totalAttempts: Int,
    val overallAccuracyPercent: Float?, // Null if totalAttempts < 5
    val totalMistakesCount: Int,
    val totalTimeSpentSeconds: Long,
    val completedTestsCount: Int,
    val revisionDueCount: Int,
    val savedContentCount: Int,
    val currentStreakDays: Int,
    val streakMessage: String,
    val isSufficientData: Boolean // True if totalAttempts >= 5
) : Serializable

/**
 * Subject Level Performance Analytics (Step 36)
 */
data class SubjectPerformanceDetail(
    val subject: String,
    val totalAttempts: Int,
    val correctCount: Int,
    val accuracyPercent: Float?, // Null if attempts < 3
    val trend: PerformanceTrend,
    val avgTimePerQuestionSeconds: Float,
    val isWeak: Boolean,
    val isStrong: Boolean,
    val isDeclining: Boolean,
    val statusMessage: String
) : Serializable

/**
 * Chapter Level Performance Analytics (Step 36)
 */
data class ChapterPerformanceDetail(
    val chapter: String,
    val subject: String,
    val accuracyPercent: Float?,
    val totalAttempts: Int,
    val hasSufficientData: Boolean
) : Serializable

/**
 * Topic Level Performance Analytics & Speed-Accuracy Matrix (Step 36)
 */
data class TopicPerformanceDetail(
    val topic: String,
    val chapter: String,
    val subject: String,
    val accuracyPercent: Float?,
    val totalAttempts: Int,
    val avgTimePerQuestionSeconds: Float,
    val hasSufficientData: Boolean,
    val isWeak: Boolean,
    val isStrong: Boolean,
    val speedAccuracyCategory: String, // "Strong (Fast & Accurate)", "Needs Speed Practice", "Needs Concept Practice", "Needs Foundational Revision"
    val speedAccuracyAdvice: String,
    val unmasteredMistakesCount: Int,
    val actionReason: String
) : Serializable

/**
 * Breakdown Item for Time-Based Daily Plan
 */
data class PlanBreakdownItem(
    val stepNumber: Int,
    val title: String,
    val durationMinutes: Int,
    val activityType: String, // "REVISION", "PRACTICE", "CURRENT_AFFAIRS", "MISTAKE_REVIEW"
    val description: String,
    val targetTopic: String,
    val targetSubject: String
) : Serializable

/**
 * Time-Based Daily Study Plan
 */
data class TimeBasedStudyPlan(
    val availableTimeOption: String, // "15 min", "30 min", "60 min", "90+ min"
    val totalMinutes: Int,
    val breakdownItems: List<PlanBreakdownItem>
) : Serializable

/**
 * Quick Study Session configuration (Step 36)
 */
data class QuickStudySession(
    val durationMinutes: Int, // 5, 10, 15
    val title: String,
    val subtitle: String,
    val focusTopic: String,
    val focusSubject: String,
    val sessionType: String, // REVISION, WEAK_TOPIC, MISTAKE, CA_QUIZ
    val questionCount: Int,
    val targetConfig: MockTestConfig
) : Serializable

/**
 * Primary Smart Recommendation on Home / Progress
 */
data class TodayFocusRecommendation(
    val title: String,
    val subtitle: String,
    val focusTopic: String,
    val focusSubject: String,
    val focusChapter: String,
    val reason: String,
    val transparencySignal: String,
    val primaryActionText: String,
    val actionType: String, // PRACTICE, REVISE, CURRENT_AFFAIRS
    val config: MockTestConfig
) : Serializable

/**
 * Goal Progress Tracker
 */
data class GoalProgress(
    val dailyQuestionsSolved: Int,
    val dailyQuestionsTarget: Int,
    val dailyQuestionsPercent: Int,
    val dailyMinutesSpent: Int,
    val dailyMinutesTarget: Int,
    val dailyMinutesPercent: Int,
    val weeklyTestsCompleted: Int,
    val weeklyTestsTarget: Int,
    val weeklyTestsPercent: Int
) : Serializable

/**
 * Comprehensive Smart Learning & Personalization Engine (Step 36)
 */
object PersonalizationEngine {

    private const val MIN_ATTEMPTS_FOR_TOPIC_DATA = 3
    private const val MIN_ATTEMPTS_FOR_OVERALL_DATA = 5

    /**
     * Builds the complete Learning Profile from real database records.
     */
    fun computeLearningProfile(
        user: UserProfile?,
        attempts: List<MockTestAttempt>,
        mistakes: List<MistakeItem>,
        topicMasteries: List<TopicMastery>,
        focusSessions: List<FocusSession>,
        savedContentCount: Int = 0,
        settings: PersonalizationSettings = PersonalizationSettings()
    ): LearningProfile {
        val selectedExam = user?.examName?.ifBlank { "RRB Group D (Railway)" } ?: "RRB Group D (Railway)"
        val selectedLanguage = user?.languagePreference?.ifBlank { "English" } ?: "English"
        val subjects = user?.subjects?.ifEmpty { listOf("Mathematics", "General Intelligence & Reasoning", "General Science", "General Awareness") }
            ?: listOf("Mathematics", "General Intelligence & Reasoning", "General Science", "General Awareness")

        val totalMocks = attempts.size
        val totalAttempts = attempts.sumOf { it.totalQuestions }
        val overallAccuracy = if (totalAttempts >= MIN_ATTEMPTS_FOR_OVERALL_DATA && attempts.isNotEmpty()) {
            attempts.map { it.accuracyPercent }.average().toFloat()
        } else null

        val unmasteredMistakesCount = mistakes.count { !it.isMastered }
        val focusSeconds = focusSessions.sumOf { it.actualMinutesSpent * 60L }
        val mockSeconds = attempts.sumOf { it.timeSpentSeconds.toLong() }
        val totalTimeSpentSeconds = focusSeconds + mockSeconds

        val revisionDueCount = topicMasteries.count {
            it.masteryState == "REVISION_DUE" ||
            it.masteryState == "WEAK" ||
            (it.recommendedReviewDateMillis > 0 && it.recommendedReviewDateMillis <= System.currentTimeMillis())
        }

        // Streak calculation based on consecutive days of actual completed tests or focus sessions
        val streakDays = computeActiveStreak(attempts, focusSessions)
        val streakMessage = if (streakDays > 0) {
            "🔥 $streakDays Day Study Streak!"
        } else {
            "Start a new study streak today."
        }

        return LearningProfile(
            selectedExam = selectedExam,
            selectedLanguage = selectedLanguage,
            subjects = subjects,
            totalAttempts = totalAttempts,
            overallAccuracyPercent = overallAccuracy,
            totalMistakesCount = unmasteredMistakesCount,
            totalTimeSpentSeconds = totalTimeSpentSeconds,
            completedTestsCount = totalMocks,
            revisionDueCount = revisionDueCount,
            savedContentCount = savedContentCount,
            currentStreakDays = streakDays,
            streakMessage = streakMessage,
            isSufficientData = totalAttempts >= MIN_ATTEMPTS_FOR_OVERALL_DATA
        )
    }

    /**
     * Compute Active Streak without guilt messaging.
     */
    private fun computeActiveStreak(
        attempts: List<MockTestAttempt>,
        focusSessions: List<FocusSession>
    ): Int {
        val activityDates = HashSet<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        attempts.forEach {
            if (it.timestamp > 0) activityDates.add(sdf.format(Date(it.timestamp)))
        }
        focusSessions.forEach {
            if (it.timestamp > 0 && it.actualMinutesSpent > 0) activityDates.add(sdf.format(Date(it.timestamp)))
        }

        if (activityDates.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        var streak = 0

        // Check if user did activity today
        val todayStr = sdf.format(calendar.time)
        val didActivityToday = activityDates.contains(todayStr)

        if (!didActivityToday) {
            // Check yesterday
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(calendar.time)
            if (!activityDates.contains(yesterdayStr)) {
                return 0
            }
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Count backward consecutive days
        while (true) {
            val dateStr = sdf.format(calendar.time)
            if (activityDates.contains(dateStr)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return if (didActivityToday) streak + 1 else streak
    }

    /**
     * Compute Subject Level Performance Details
     */
    fun computeSubjectPerformances(
        user: UserProfile?,
        attempts: List<MockTestAttempt>,
        topicMasteries: List<TopicMastery>
    ): List<SubjectPerformanceDetail> {
        val subjects = user?.subjects?.ifEmpty {
            listOf("Mathematics", "General Intelligence & Reasoning", "General Science", "General Awareness")
        } ?: listOf("Mathematics", "General Intelligence & Reasoning", "General Science", "General Awareness")

        return subjects.map { subject ->
            val subAttempts = attempts.filter { it.subject.equals(subject, ignoreCase = true) || it.subject == "All Subjects" }
            val totalQs = subAttempts.sumOf { it.totalQuestions }

            if (totalQs < MIN_ATTEMPTS_FOR_TOPIC_DATA) {
                SubjectPerformanceDetail(
                    subject = subject,
                    totalAttempts = totalQs,
                    correctCount = subAttempts.sumOf { it.correctCount },
                    accuracyPercent = null,
                    trend = PerformanceTrend.INSUFFICIENT_DATA,
                    avgTimePerQuestionSeconds = 0f,
                    isWeak = false,
                    isStrong = false,
                    isDeclining = false,
                    statusMessage = "$subject: Not enough data yet — complete a few more questions."
                )
            } else {
                val acc = subAttempts.map { it.accuracyPercent }.average().toFloat()
                val sortedSubMocks = subAttempts.sortedByDescending { it.timestamp }
                val latestAcc = sortedSubMocks.first().accuracyPercent
                val prevAcc = if (sortedSubMocks.size > 1) sortedSubMocks.drop(1).map { it.accuracyPercent }.average().toFloat() else null

                val diff = if (prevAcc != null) latestAcc - prevAcc else 0f
                val trend = when {
                    prevAcc == null -> PerformanceTrend.INSUFFICIENT_DATA
                    diff >= 3f -> PerformanceTrend.IMPROVING
                    diff <= -3f -> PerformanceTrend.DECLINING
                    else -> PerformanceTrend.STABLE
                }

                val totalTime = subAttempts.sumOf { it.timeSpentSeconds }
                val avgTime = if (totalQs > 0) totalTime.toFloat() / totalQs else 45f

                val isDeclining = trend == PerformanceTrend.DECLINING
                val isWeak = acc < 60f || isDeclining
                val isStrong = acc >= 75f && !isDeclining

                val msg = when {
                    isWeak && isDeclining -> "$subject: Needs Attention (Accuracy: ${acc.toInt()}%, declining recently)."
                    isWeak -> "$subject: Accuracy at ${acc.toInt()}%. Practice recommended."
                    isStrong -> "$subject: Strong area (${acc.toInt()}% accuracy)."
                    else -> "$subject: Stable performance at ${acc.toInt()}%."
                }

                SubjectPerformanceDetail(
                    subject = subject,
                    totalAttempts = totalQs,
                    correctCount = subAttempts.sumOf { it.correctCount },
                    accuracyPercent = acc,
                    trend = trend,
                    avgTimePerQuestionSeconds = avgTime,
                    isWeak = isWeak,
                    isStrong = isStrong,
                    isDeclining = isDeclining,
                    statusMessage = msg
                )
            }
        }
    }

    /**
     * Compute Chapter Level Performance Details
     */
    fun computeChapterPerformances(
        attempts: List<MockTestAttempt>,
        topicMasteries: List<TopicMastery>
    ): List<ChapterPerformanceDetail> {
        val chapterMap = mutableMapOf<Pair<String, String>, MutableList<MockTestAttempt>>()

        attempts.forEach { attempt ->
            val chap = "Core Concepts"
            val key = Pair(attempt.subject, chap)
            chapterMap.getOrPut(key) { mutableListOf() }.add(attempt)
        }

        topicMasteries.forEach { m ->
            val chap = m.chapter.ifBlank { "Core Concepts" }
            val key = Pair(m.subject, chap)
            if (!chapterMap.containsKey(key)) {
                chapterMap[key] = mutableListOf()
            }
        }

        return chapterMap.map { (key, mList) ->
            val totalQs = mList.sumOf { it.totalQuestions }
            val hasEnough = totalQs >= MIN_ATTEMPTS_FOR_TOPIC_DATA
            val acc = if (hasEnough) mList.map { it.accuracyPercent }.average().toFloat() else null

            ChapterPerformanceDetail(
                chapter = key.second,
                subject = key.first,
                accuracyPercent = acc,
                totalAttempts = totalQs,
                hasSufficientData = hasEnough
            )
        }
    }

    /**
     * Compute Topic Level Details with Speed-Accuracy Matrix
     */
    fun computeTopicPerformances(
        attempts: List<MockTestAttempt>,
        mistakes: List<MistakeItem>,
        topicMasteries: List<TopicMastery>
    ): List<TopicPerformanceDetail> {
        val topicMap = mutableMapOf<String, TopicPerformanceAccumulator>()

        attempts.forEach { a ->
            if (a.topic.isNotBlank() && a.topic != "All Topics") {
                val acc = topicMap.getOrPut(a.topic) { TopicPerformanceAccumulator(topic = a.topic, subject = a.subject, chapter = "Core Concepts") }
                acc.totalQs += a.totalQuestions
                acc.correctQs += a.correctCount
                acc.totalSeconds += a.timeSpentSeconds
            }
        }

        mistakes.forEach { m ->
            if (m.topic.isNotBlank()) {
                val acc = topicMap.getOrPut(m.topic) { TopicPerformanceAccumulator(topic = m.topic, subject = m.subject, chapter = "General") }
                if (!m.isMastered) acc.unmasteredMistakes++
            }
        }

        topicMasteries.forEach { tm ->
            if (tm.topic.isNotBlank()) {
                val acc = topicMap.getOrPut(tm.topic) { TopicPerformanceAccumulator(topic = tm.topic, subject = tm.subject, chapter = tm.chapter) }
                if (acc.totalQs == 0 && tm.totalQuestionsAttempted > 0) {
                    acc.totalQs = tm.totalQuestionsAttempted
                    acc.correctQs = tm.correctQuestionsCount
                }
            }
        }

        return topicMap.values.map { acc ->
            val hasData = acc.totalQs >= MIN_ATTEMPTS_FOR_TOPIC_DATA
            val accuracy = if (hasData && acc.totalQs > 0) (acc.correctQs.toFloat() / acc.totalQs) * 100f else null
            val avgTime = if (acc.totalQs > 0) acc.totalSeconds.toFloat() / acc.totalQs else 45f

            val (speedCat, speedAdv) = if (accuracy == null) {
                Pair("Not Enough Data", "Complete a few more questions on this topic to unlock timing insights.")
            } else {
                when {
                    accuracy >= 75f && avgTime <= 45f -> Pair("Strong (Fast & Accurate)", "Excellent performance! Maintain accuracy with periodic revision.")
                    accuracy >= 75f && avgTime > 45f -> Pair("Needs Speed Practice", "High accuracy, but time taken is higher than optimal. Practice speed drills.")
                    accuracy < 60f && avgTime <= 45f -> Pair("Needs Concept Practice", "Fast responses with lower accuracy indicate rushing. Review core concepts.")
                    else -> Pair("Needs Foundational Revision", "Lower accuracy and high time spent. Go through step-by-step theory explanation.")
                }
            }

            val isWeak = accuracy != null && (accuracy < 60f || acc.unmasteredMistakes >= 2)
            val isStrong = accuracy != null && accuracy >= 75f && acc.unmasteredMistakes < 2

            val reason = when {
                !hasData -> "Not enough data yet (${acc.totalQs} attempts)"
                isWeak -> "Recent accuracy is ${accuracy?.toInt()}% with ${acc.unmasteredMistakes} logged mistakes."
                isStrong -> "Strong accuracy of ${accuracy?.toInt()}% across ${acc.totalQs} attempts."
                else -> "Moderate accuracy of ${accuracy?.toInt()}%."
            }

            TopicPerformanceDetail(
                topic = acc.topic,
                chapter = acc.chapter,
                subject = acc.subject,
                accuracyPercent = accuracy,
                totalAttempts = acc.totalQs,
                avgTimePerQuestionSeconds = avgTime,
                hasSufficientData = hasData,
                isWeak = isWeak,
                isStrong = isStrong,
                speedAccuracyCategory = speedCat,
                speedAccuracyAdvice = speedAdv,
                unmasteredMistakesCount = acc.unmasteredMistakes,
                actionReason = reason
            )
        }
    }

    private class TopicPerformanceAccumulator(
        val topic: String,
        val subject: String,
        val chapter: String,
        var totalQs: Int = 0,
        var correctQs: Int = 0,
        var totalSeconds: Int = 0,
        var unmasteredMistakes: Int = 0
    )

    /**
     * Generate Time-Based Daily Study Plan
     */
    fun generateTimeBasedStudyPlan(
        option: String, // "15 min", "30 min", "60 min", "90+ min"
        weakTopics: List<TopicPerformanceDetail>,
        topicMasteries: List<TopicMastery>,
        user: UserProfile?
    ): TimeBasedStudyPlan {
        val primarySubject = user?.subjects?.firstOrNull() ?: "Mathematics"
        val weak = weakTopics.firstOrNull { it.isWeak }
        val weakTopicName = weak?.topic ?: "Percentage & Core Calculations"
        val weakSubName = weak?.subject ?: primarySubject

        val dueTopic = topicMasteries.firstOrNull { it.masteryState == "REVISION_DUE" }?.topic ?: "General Science & Motion"

        return when (option) {
            "15 min" -> TimeBasedStudyPlan(
                availableTimeOption = "15 min",
                totalMinutes = 15,
                breakdownItems = listOf(
                    PlanBreakdownItem(1, "5-Min Quick Spaced Revision", 5, "REVISION", "Review high-yield facts for $dueTopic", dueTopic, weakSubName),
                    PlanBreakdownItem(2, "10-Min Weak Topic Practice Drill", 10, "PRACTICE", "Solve 8 targeted MCQs on $weakTopicName", weakTopicName, weakSubName)
                )
            )
            "60 min" -> TimeBasedStudyPlan(
                availableTimeOption = "60 min",
                totalMinutes = 60,
                breakdownItems = listOf(
                    PlanBreakdownItem(1, "15-Min Spaced Revision Session", 15, "REVISION", "Revisit flashcards & formulas for $dueTopic", dueTopic, weakSubName),
                    PlanBreakdownItem(2, "30-Min Focused Weak Area Practice", 30, "PRACTICE", "Solve 20 CBT questions on $weakTopicName", weakTopicName, weakSubName),
                    PlanBreakdownItem(3, "15-Min Current Affairs & GK Quiz", 15, "CURRENT_AFFAIRS", "Solve 10 fresh exam-oriented GK questions", "Recent Current Affairs", "General Awareness")
                )
            )
            "90+ min" -> TimeBasedStudyPlan(
                availableTimeOption = "90+ min",
                totalMinutes = 90,
                breakdownItems = listOf(
                    PlanBreakdownItem(1, "20-Min Deep Spaced Repetition", 20, "REVISION", "Comprehensive formula & rule review for $dueTopic", dueTopic, weakSubName),
                    PlanBreakdownItem(2, "45-Min CBT Mock Practice Drill", 45, "PRACTICE", "Full speed-accuracy drill on $weakTopicName & $primarySubject", weakTopicName, weakSubName),
                    PlanBreakdownItem(3, "15-Min Current Affairs Quiz", 15, "CURRENT_AFFAIRS", "Daily 10-Question Current Affairs Brief", "National Policy & GK", "General Awareness"),
                    PlanBreakdownItem(4, "10-Min Mistake Book Review", 10, "MISTAKE_REVIEW", "Review unmastered mistake log explanations", "Mistake Analysis", weakSubName)
                )
            )
            else -> // Default 30 min
                TimeBasedStudyPlan(
                    availableTimeOption = "30 min",
                    totalMinutes = 30,
                    breakdownItems = listOf(
                        PlanBreakdownItem(1, "10-Min Spaced Concept Revision", 10, "REVISION", "Revisit core formulas & key points for $dueTopic", dueTopic, weakSubName),
                        PlanBreakdownItem(2, "15-Min Targeted Practice Drill", 15, "PRACTICE", "Solve 12 MCQs on $weakTopicName", weakTopicName, weakSubName),
                        PlanBreakdownItem(3, "5-Min Current Affairs Brief", 5, "CURRENT_AFFAIRS", "Take 5-question daily news mini-quiz", "Daily Current Affairs", "General Awareness")
                    )
                )
        }
    }

    /**
     * Create Quick Study Session Configuration
     */
    fun createQuickStudySession(
        durationMinutes: Int, // 5, 10, 15
        weakTopics: List<TopicPerformanceDetail>,
        topicMasteries: List<TopicMastery>,
        user: UserProfile?
    ): QuickStudySession {
        val exam = user?.examName?.ifBlank { "RRB Group D" } ?: "RRB Group D"
        val weak = weakTopics.firstOrNull { it.isWeak }
        val weakTopicName = weak?.topic ?: "Percentage"
        val weakSubject = weak?.subject ?: (user?.subjects?.firstOrNull() ?: "Mathematics")

        val qCount = when (durationMinutes) {
            5 -> 5
            10 -> 10
            else -> 15
        }

        val config = MockTestConfig(
            exam = exam,
            testType = MockTestType.SMART_PRACTICE,
            questionSource = QuestionSourceType.CHAPTER_PRACTICE,
            subject = weakSubject,
            chapter = weak?.chapter ?: "All Chapters",
            topic = weakTopicName,
            questionCount = qCount,
            timeLimitMinutes = durationMinutes,
            language = user?.languagePreference ?: "English"
        )

        return QuickStudySession(
            durationMinutes = durationMinutes,
            title = "⚡ Quick Study ($durationMinutes min)",
            subtitle = "$qCount Questions on $weakTopicName ($weakSubject)",
            focusTopic = weakTopicName,
            focusSubject = weakSubject,
            sessionType = "WEAK_TOPIC",
            questionCount = qCount,
            targetConfig = config
        )
    }

    /**
     * Compute Primary Today's Focus Recommendation
     */
    fun computeTodayFocusRecommendation(
        user: UserProfile?,
        weakTopics: List<TopicPerformanceDetail>,
        topicMasteries: List<TopicMastery>,
        settings: PersonalizationSettings = PersonalizationSettings()
    ): TodayFocusRecommendation {
        val exam = user?.examName?.ifBlank { "RRB Group D" } ?: "RRB Group D"

        if (!settings.isEnabled) {
            val defaultSubject = user?.subjects?.firstOrNull() ?: "Mathematics"
            return TodayFocusRecommendation(
                title = "Standard Exam Practice",
                subtitle = "Practice high-yield questions for $defaultSubject",
                focusTopic = "All Topics",
                focusSubject = defaultSubject,
                focusChapter = "All Chapters",
                reason = "Personalization is currently disabled. Showing standard syllabus practice.",
                transparencySignal = "Standard curriculum mode.",
                primaryActionText = "Start Practice",
                actionType = "PRACTICE",
                config = MockTestConfig(exam = exam, subject = defaultSubject, testType = MockTestType.SUBJECT_PRACTICE)
            )
        }

        val weak = weakTopics.firstOrNull { it.isWeak }
        if (weak != null) {
            return TodayFocusRecommendation(
                title = "Targeted Weak Topic Practice",
                subtitle = "Focus on '${weak.topic}' in ${weak.subject}",
                focusTopic = weak.topic,
                focusSubject = weak.subject,
                focusChapter = weak.chapter,
                reason = "Your recent accuracy on '${weak.topic}' is ${weak.accuracyPercent?.toInt() ?: 50}%, which is lower than your subject average.",
                transparencySignal = "Based on your actual test history (${weak.totalAttempts} attempts, ${weak.unmasteredMistakesCount} logged mistakes).",
                primaryActionText = "Start Practice",
                actionType = "PRACTICE",
                config = MockTestConfig(
                    exam = exam,
                    testType = MockTestType.SMART_PRACTICE,
                    questionSource = QuestionSourceType.CHAPTER_PRACTICE,
                    subject = weak.subject,
                    chapter = weak.chapter,
                    topic = weak.topic,
                    questionCount = 15,
                    timeLimitMinutes = 20
                )
            )
        }

        val due = topicMasteries.firstOrNull { it.masteryState == "REVISION_DUE" }
        if (due != null) {
            return TodayFocusRecommendation(
                title = "Spaced Revision Due",
                subtitle = "Revisit '${due.topic}' in ${due.subject}",
                focusTopic = due.topic,
                focusSubject = due.subject,
                focusChapter = due.chapter,
                reason = "It has been over 3 days since you last reviewed '${due.topic}'.",
                transparencySignal = "Triggered by your spaced repetition review schedule.",
                primaryActionText = "Revise Now",
                actionType = "REVISE",
                config = MockTestConfig(
                    exam = exam,
                    testType = MockTestType.REVISION_TEST,
                    subject = due.subject,
                    topic = due.topic,
                    questionCount = 10,
                    timeLimitMinutes = 15
                )
            )
        }

        // Fallback for insufficient data
        val defaultSubject = user?.subjects?.firstOrNull() ?: "General Awareness"
        return TodayFocusRecommendation(
            title = "Current Affairs Practice",
            subtitle = "Solve today's top 10 current affairs MCQs",
            focusTopic = "Recent Current Affairs",
            focusSubject = defaultSubject,
            focusChapter = "General Awareness",
            reason = "Take a quick practice quiz to help unlock personalized performance insights.",
            transparencySignal = "Not enough data yet for topic weakness detection. Complete a few more practice questions.",
            primaryActionText = "Start CA Quiz",
            actionType = "CURRENT_AFFAIRS",
            config = MockTestConfig(
                exam = exam,
                testType = MockTestType.MIXED_PRACTICE,
                questionSource = QuestionSourceType.CURRENT_AFFAIRS,
                subject = defaultSubject,
                questionCount = 10,
                timeLimitMinutes = 15
            )
        )
    }

    /**
     * Compute Goal Progress
     */
    fun computeGoalProgress(
        user: UserProfile?,
        attempts: List<MockTestAttempt>,
        focusSessions: List<FocusSession>,
        settings: PersonalizationSettings = PersonalizationSettings()
    ): GoalProgress {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val todayMocks = attempts.filter { sdf.format(Date(it.timestamp)) == todayStr }
        val todayFocus = focusSessions.filter { sdf.format(Date(it.timestamp)) == todayStr }

        val todayQuestions = todayMocks.sumOf { it.totalQuestions }
        val todayMinutes = todayFocus.sumOf { it.actualMinutesSpent } + todayMocks.sumOf { it.timeSpentSeconds / 60 }

        val qTarget = settings.dailyQuestionGoal.coerceAtLeast(10)
        val mTarget = settings.dailyStudyMinutesGoal.coerceAtLeast(15)
        val tTarget = settings.weeklyTestsGoal.coerceAtLeast(1)

        val qPct = ((todayQuestions.toFloat() / qTarget) * 100).toInt().coerceAtMost(100)
        val mPct = ((todayMinutes.toFloat() / mTarget) * 100).toInt().coerceAtMost(100)

        // Weekly tests count
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekCutoff = cal.timeInMillis
        val weeklyTests = attempts.count { it.timestamp >= weekCutoff }
        val wPct = ((weeklyTests.toFloat() / tTarget) * 100).toInt().coerceAtMost(100)

        return GoalProgress(
            dailyQuestionsSolved = todayQuestions,
            dailyQuestionsTarget = qTarget,
            dailyQuestionsPercent = qPct,
            dailyMinutesSpent = todayMinutes,
            dailyMinutesTarget = mTarget,
            dailyMinutesPercent = mPct,
            weeklyTestsCompleted = weeklyTests,
            weeklyTestsTarget = tTarget,
            weeklyTestsPercent = wPct
        )
    }
}
