package com.example.service.intelligence

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Step 50: Nova Smart Study Intelligence Engine
 * Computes missions, weak topics, adaptive schedule suggestions, weekly reviews, and streak stats.
 */
class NovaStudyIntelligenceEngine {

    companion object {
        const val MIN_WEAK_TOPIC_ATTEMPTS = 5
        const val WEAK_TOPIC_ACCURACY_THRESHOLD = 60 // %
        const val MIN_DAILY_MINUTES_FOR_STREAK = 20
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

    fun getTodayFormatted(): String = dateFormat.format(Date())

    fun getTodayDayOfWeek(): String {
        return dayOfWeekFormat.format(Date()).uppercase(Locale.getDefault()).take(3) // "MON", "TUE", etc.
    }

    /**
     * Step 50 (1 & 2): Generate or resolve Today's Mission tasks respecting user schedule & preferences.
     */
    fun buildDailyMissionsForToday(
        existingMissions: List<DailyMissionTask>,
        scheduleItems: List<StudyScheduleItem>,
        userProfile: UserProfile?
    ): List<DailyMissionTask> {
        val todayStr = getTodayFormatted()
        if (existingMissions.isNotEmpty()) {
            return existingMissions
        }

        val todayDay = getTodayDayOfWeek() // e.g., "MON"
        val activeScheduleItems = scheduleItems.filter { item ->
            !item.isPaused && (item.repeatDaysJson.contains(todayDay, ignoreCase = true) || item.dayOfWeek.equals(todayDay, ignoreCase = true))
        }

        val generated = mutableListOf<DailyMissionTask>()

        if (activeScheduleItems.isNotEmpty()) {
            // Respect user schedule
            activeScheduleItems.forEach { schedule ->
                generated.add(
                    DailyMissionTask(
                        userId = userProfile?.id ?: "current_user",
                        title = "${schedule.subject} — ${schedule.durationMinutes} min",
                        subject = schedule.subject,
                        topic = schedule.topic,
                        actionType = "FOCUS",
                        targetMinutes = schedule.durationMinutes,
                        isFromSchedule = true,
                        scheduledTime = schedule.startTime,
                        dateFormatted = todayStr
                    )
                )
            }
        } else {
            // Fallback suggestions based on selected subjects & exam preferences
            val subjects = userProfile?.subjects?.ifEmpty { listOf("Mathematics", "Reasoning", "General Knowledge") }
                ?: listOf("Mathematics", "Reasoning", "General Knowledge")

            val sub1 = subjects.getOrElse(0) { "Mathematics" }
            val sub2 = subjects.getOrElse(1) { "Reasoning" }

            generated.add(
                DailyMissionTask(
                    userId = userProfile?.id ?: "current_user",
                    title = "$sub1 — 45 min",
                    subject = sub1,
                    topic = "Core Practice",
                    actionType = "FOCUS",
                    targetMinutes = 45,
                    isFromSchedule = false,
                    scheduledTime = "06:00 PM",
                    dateFormatted = todayStr
                )
            )

            generated.add(
                DailyMissionTask(
                    userId = userProfile?.id ?: "current_user",
                    title = "$sub2 Practice — 30 min",
                    subject = sub2,
                    topic = "Active Problem Solving",
                    actionType = "PRACTICE",
                    targetMinutes = 30,
                    isFromSchedule = false,
                    scheduledTime = "07:30 PM",
                    dateFormatted = todayStr
                )
            )

            generated.add(
                DailyMissionTask(
                    userId = userProfile?.id ?: "current_user",
                    title = "Current Affairs — 20 min",
                    subject = "General Awareness",
                    topic = "Today's Updates",
                    actionType = "CURRENT_AFFAIRS",
                    targetMinutes = 20,
                    isFromSchedule = false,
                    scheduledTime = "09:00 PM",
                    dateFormatted = todayStr
                )
            )
        }

        return generated
    }

    /**
     * Step 50 (4): Weak Topic Detection from actual question attempts & performance.
     */
    fun detectWeakTopics(
        attempts: List<QuestionHistoryEntity>,
        mistakes: List<MistakeItem>
    ): List<WeakTopicInsight> {
        if (attempts.size < MIN_WEAK_TOPIC_ATTEMPTS) {
            return emptyList()
        }

        // Group by subject and topic
        val topicStats = mutableMapOf<String, Pair<Int, Int>>() // topicKey -> (totalAttempts, correctCount)

        attempts.forEach { att ->
            val key = "${att.subject}|${att.topic.ifEmpty { "General" }}"
            val current = topicStats.getOrDefault(key, Pair(0, 0))
            val isCorrect = att.lastResult == "CORRECT" || att.correctCount > 0
            topicStats[key] = Pair(current.first + att.attemptCount.coerceAtLeast(1), current.second + if (isCorrect) att.correctCount.coerceAtLeast(1) else 0)
        }

        val insights = mutableListOf<WeakTopicInsight>()

        topicStats.forEach { (key, stats) ->
            val total = stats.first
            val correct = stats.second
            if (total >= MIN_WEAK_TOPIC_ATTEMPTS) {
                val acc = (correct * 100) / total
                if (acc <= WEAK_TOPIC_ACCURACY_THRESHOLD) {
                    val parts = key.split("|")
                    val sub = parts.getOrElse(0) { "Subject" }
                    val top = parts.getOrElse(1) { "Topic" }
                    insights.add(
                        WeakTopicInsight(
                            subject = sub,
                            topic = top,
                            totalAttempts = total,
                            accuracyPercentage = acc,
                            recommendedPracticeMinutes = 25,
                            insightHinglish = "🧠 Nova Insight: $top me practice ki zaroorat hai. Recent accuracy: $acc%. Recommended: 20–30 minutes targeted practice."
                        )
                    )
                }
            }
        }

        return insights.take(3)
    }

    /**
     * Step 50 (6): Adaptive Schedule Shift Suggestions based on missed session patterns.
     */
    fun analyzeAdaptiveScheduleShifts(
        scheduleItems: List<StudyScheduleItem>,
        scheduleLogs: List<StudyScheduleLog>
    ): AdaptiveScheduleShiftSuggestion? {
        val missedLogs = scheduleLogs.filter { it.status == "MISSED" }
        if (missedLogs.size < 2) return null

        val missedByScheduleId = missedLogs.groupBy { it.scheduleId }
        val frequentMissed = missedByScheduleId.entries.firstOrNull { it.value.size >= 2 && it.key.isNotEmpty() }
            ?: return null

        val item = scheduleItems.find { it.id == frequentMissed.key } ?: return null
        val currentTimeStr = item.startTime

        // Suggest shifting by +1 hour
        val suggestedTimeStr = calculateShiftedTime(currentTimeStr, shiftHours = 1)

        return AdaptiveScheduleShiftSuggestion(
            scheduleId = item.id,
            subject = item.subject,
            currentTime = currentTimeStr,
            suggestedTime = suggestedTimeStr,
            missedCount = frequentMissed.value.size,
            hinglishMessage = "🧠 Bhai, tum $currentTimeStr ${item.subject} sessions often miss kar rahe ho. Kya ise $suggestedTimeStr shift karein?"
        )
    }

    private fun calculateShiftedTime(timeStr: String, shiftHours: Int): String {
        return try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(timeStr) ?: return "08:00 PM"
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.HOUR_OF_DAY, shiftHours)
            sdf.format(cal.time)
        } catch (e: Exception) {
            "08:00 PM"
        }
    }

    /**
     * Step 50 (7): Weekly Study Review statistics.
     */
    fun calculateWeeklyReviewStats(
        focusSessions: List<FocusSession>,
        scheduleItems: List<StudyScheduleItem>,
        scheduleLogs: List<StudyScheduleLog>,
        mockAttempts: List<MockTestAttempt>,
        questionAttempts: List<QuestionHistoryEntity>
    ): WeeklyReviewStats {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - 7L * 24 * 60 * 60 * 1000

        val recentFocus = focusSessions.filter { it.timestamp >= oneWeekAgo }
        val recentLogs = scheduleLogs.filter { it.timestamp >= oneWeekAgo }
        val recentMocks = mockAttempts.filter { it.timestamp >= oneWeekAgo }
        val recentQuestions = questionAttempts.filter { it.lastAttemptedAt >= oneWeekAgo }

        val completedFocusMinutes = recentFocus.sumOf { it.actualMinutesSpent }
        val plannedLogMinutes = recentLogs.sumOf { it.plannedMinutes }.ifZero(360)

        val totalCompletedMinutes = completedFocusMinutes.coerceAtLeast(recentLogs.filter { it.status == "COMPLETED" }.sumOf { it.actualMinutesSpent })

        val plannedHours = (plannedLogMinutes / 60.0f).coerceAtLeast(6.0f)
        val completedHours = (totalCompletedMinutes / 60.0f)

        val completionPct = ((completedHours / plannedHours) * 100).toInt().coerceIn(0, 100)

        // Subject wise breakdown
        val subjectMinutesMap = mutableMapOf<String, Int>()
        recentFocus.forEach { sess ->
            val sub = sess.subject.ifEmpty { "General Study" }
            subjectMinutesMap[sub] = (subjectMinutesMap[sub] ?: 0) + sess.actualMinutesSpent
        }

        val bestSub = subjectMinutesMap.maxByOrNull { it.value }?.key ?: "Mathematics"
        val needsAttnSub = if (subjectMinutesMap.size > 1) {
            subjectMinutesMap.minByOrNull { it.value }?.key ?: "Reasoning"
        } else {
            "Reasoning"
        }

        val correctQ = recentQuestions.count { it.lastResult == "CORRECT" || it.correctCount > 0 }
        val totalQ = recentQuestions.size
        val practiceAcc = if (totalQ > 0) (correctQ * 100) / totalQ else 75

        val missedCount = recentLogs.count { it.status == "MISSED" }

        return WeeklyReviewStats(
            plannedHours = plannedHours,
            completedHours = completedHours,
            completionPercentage = completionPct,
            bestSubject = bestSub,
            needsAttentionSubject = needsAttnSub,
            totalFocusSessions = recentFocus.size,
            practiceAccuracy = practiceAcc,
            mockTestsCompleted = recentMocks.size,
            missedSessionsCount = missedCount,
            subjectWiseMinutes = subjectMinutesMap
        )
    }

    private fun Int.ifZero(defaultVal: Int): Int = if (this == 0) defaultVal else this
}
