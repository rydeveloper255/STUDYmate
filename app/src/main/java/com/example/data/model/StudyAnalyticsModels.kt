package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Step 56: Study Analytics & Smart Insights Engine 2.0 Models
 */

enum class StudyEventType {
    FOCUS_STARTED,
    FOCUS_COMPLETED,
    FOCUS_INTERRUPTED,
    FOCUS_CANCELLED,
    SESSION_PAUSED,
    SESSION_RESUMED,
    SCHEDULE_COMPLETED,
    SCHEDULE_MISSED,
    SCHEDULE_SKIPPED,
    GOAL_CREATED,
    GOAL_COMPLETED
}

@Entity(tableName = "study_events")
data class StudyEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "current_user",
    val eventType: String, // StudyEventType.name
    val subject: String = "",
    val topic: String = "",
    val sessionId: String = "",
    val scheduleId: String = "",
    val goalId: String = "",
    val plannedDurationMinutes: Int = 0,
    val actualDurationMinutes: Int = 0,
    val focusMode: String = "STANDARD",
    val strictMode: Boolean = false,
    val metadataJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)

data class StudySessionData(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String = "current_user",
    val subject: String,
    val topic: String = "",
    val startTime: Long,
    val endTime: Long = System.currentTimeMillis(),
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val completionStatus: String, // "COMPLETED", "INTERRUPTED", "CANCELLED", "PAUSED"
    val focusMode: String = "STANDARD",
    val strictMode: Boolean = false,
    val scheduleId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class DailyAnalytics(
    val dateFormatted: String, // e.g. "2026-08-26"
    val timestampMillis: Long = System.currentTimeMillis(),
    val totalStudyMinutes: Int = 0,
    val scheduledStudyMinutes: Int = 0,
    val completedSessions: Int = 0,
    val interruptedSessions: Int = 0,
    val missedSessions: Int = 0,
    val subjectBreakdown: Map<String, Int> = emptyMap(),
    val focusCompletionRate: Int = 0 // 0-100 percentage
)

data class WeeklyAnalytics(
    val startDateFormatted: String,
    val endDateFormatted: String,
    val totalStudyMinutes: Int = 0,
    val averageDailyMinutes: Int = 0,
    val completedSessions: Int = 0,
    val completionRate: Int = 0,
    val strongestStudyDay: String = "N/A",
    val weakestStudyDay: String = "N/A",
    val subjectDistribution: Map<String, Int> = emptyMap(),
    val plannedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val uncompletedScheduledMinutes: Int = 0
)

data class MonthlyAnalytics(
    val monthYearFormatted: String,
    val totalStudyMinutes: Int = 0,
    val averageDailyMinutes: Int = 0,
    val sessionsCompleted: Int = 0,
    val goalsCompleted: Int = 0,
    val subjectDistribution: Map<String, Int> = emptyMap(),
    val studyConsistencyPercentage: Int = 0
)

data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val streakStartDateMillis: Long = 0L,
    val lastQualifiedStudyDateMillis: Long = 0L,
    val isQualifiedToday: Boolean = false,
    val timezoneId: String = "system"
)

enum class SubjectTrendDirection {
    UP,
    DOWN,
    STABLE,
    INSUFFICIENT_DATA
}

data class SubjectAnalytics(
    val subject: String,
    val totalMinutes: Int = 0,
    val sessionsCount: Int = 0,
    val averageSessionMinutes: Int = 0,
    val completionRate: Int = 0,
    val plannedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val trend: SubjectTrendDirection = SubjectTrendDirection.INSUFFICIENT_DATA
)

data class TimeOfDayDistribution(
    val morningMinutes: Int = 0,   // 05:00 - 12:00
    val afternoonMinutes: Int = 0, // 12:00 - 17:00
    val eveningMinutes: Int = 0,   // 17:00 - 21:00
    val nightMinutes: Int = 0,     // 21:00 - 05:00
    val dominantPeriod: String = "Not enough data"
)

enum class InsightType {
    CONSISTENCY,
    SUBJECT_BALANCE,
    SCHEDULE_ADHERENCE,
    FOCUS_COMPLETION,
    TIME_PATTERN,
    GOAL_PROGRESS,
    STUDY_TREND
}

enum class InsightSeverity {
    INFO,
    POSITIVE,
    WARNING
}

enum class InsightConfidence {
    HIGH_CONFIDENCE,
    MEDIUM_CONFIDENCE,
    INSUFFICIENT_DATA
}

data class SmartInsight(
    val id: String = UUID.randomUUID().toString(),
    val type: InsightType,
    val severity: InsightSeverity = InsightSeverity.INFO,
    val confidence: InsightConfidence = InsightConfidence.MEDIUM_CONFIDENCE,
    val title: String,
    val summary: String,
    val evidence: String, // Internal explainable facts (e.g. "Maths=380m, Physics=190m")
    val period: String = "WEEKLY", // "TODAY", "WEEKLY", "MONTHLY"
    val createdAt: Long = System.currentTimeMillis()
)

data class GoalAnalytics(
    val goalId: String,
    val title: String,
    val goalTarget: Int,
    val goalProgress: Int,
    val progressPercentage: Int,
    val deadlineMillis: Long = 0L,
    val daysRemaining: Int = 0,
    val completionStatus: String = "IN_PROGRESS",
    val isAtRisk: Boolean = false,
    val riskNotice: String? = null
)

enum class AnalyticsDateFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}
