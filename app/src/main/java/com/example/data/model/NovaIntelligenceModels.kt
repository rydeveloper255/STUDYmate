package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Step 50: Nova Smart Study Intelligence Models
 */

@Entity(tableName = "daily_mission_tasks")
data class DailyMissionTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "current_user",
    val title: String = "",
    val subject: String = "",
    val topic: String = "",
    val actionType: String = "FOCUS", // FOCUS, PRACTICE, CURRENT_AFFAIRS, MOCK, REVISION
    val targetMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val isFromSchedule: Boolean = true,
    val scheduledTime: String = "",
    val dateFormatted: String = "", // e.g. "2026-08-26"
    val completedTimestamp: Long = 0L,
    val isDismissed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_weekly_goals")
data class UserWeeklyGoalEntity(
    @PrimaryKey val userId: String = "current_user",
    val targetHoursPerWeek: Float = 10.0f,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "motivation_history")
data class MotivationHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "current_user",
    val category: String = "GENERAL_ENCOURAGEMENT",
    val messageHinglish: String = "",
    val messageHash: String = "",
    val sentTimestamp: Long = System.currentTimeMillis()
)

enum class MotivationCategory {
    FOCUS_COMPLETED,
    DAILY_GOAL_COMPLETED,
    STREAK_MILESTONE,
    STUDY_COMEBACK,
    MISSED_SESSION,
    WEEKLY_GOAL_PROGRESS,
    PRACTICE_IMPROVEMENT,
    MOCK_TEST_IMPROVEMENT,
    EXAM_PREPARATION,
    GENERAL_ENCOURAGEMENT
}

data class WeakTopicInsight(
    val subject: String,
    val topic: String,
    val totalAttempts: Int,
    val accuracyPercentage: Int,
    val recommendedPracticeMinutes: Int = 25,
    val insightHinglish: String = ""
)

data class NovaSmartRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "WEAK_TOPIC", // WEAK_TOPIC, REVISION, MOCK_TEST, MISSED_SESSION, CURRENT_AFFAIRS, SCHEDULE_SHIFT
    val title: String,
    val descriptionHinglish: String,
    val actionLabel: String, // e.g., "Practice Now", "Start Focus", "Move to 8 PM"
    val actionType: String,
    val targetSubject: String = "",
    val targetTopic: String = "",
    val targetMinutes: Int = 30,
    val suggestedTime: String = "",
    val logId: String = "",
    val scheduleId: String = "",
    val isDismissed: Boolean = false
)

data class WeeklyReviewStats(
    val plannedHours: Float = 12.0f,
    val completedHours: Float = 9.3f,
    val completionPercentage: Int = 78,
    val bestSubject: String = "Mathematics",
    val needsAttentionSubject: String = "Reasoning",
    val totalFocusSessions: Int = 14,
    val practiceAccuracy: Int = 76,
    val mockTestsCompleted: Int = 2,
    val missedSessionsCount: Int = 1,
    val subjectWiseMinutes: Map<String, Int> = emptyMap()
)

data class AdaptiveScheduleShiftSuggestion(
    val scheduleId: String,
    val subject: String,
    val currentTime: String,
    val suggestedTime: String,
    val missedCount: Int,
    val hinglishMessage: String
)

data class NovaDailySummary(
    val dateFormatted: String,
    val totalStudyMinutes: Int,
    val focusSessionsCount: Int,
    val questionsAttempted: Int,
    val practiceAccuracy: Int,
    val topSubject: String,
    val missedSessionSubject: String?,
    val eodInsightHinglish: String
)
