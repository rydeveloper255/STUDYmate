package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ScheduleRepeatType {
    ONCE, DAILY, WEEKLY, SELECTED_DAYS
}

@Entity(tableName = "study_schedule_items")
data class StudyScheduleItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "current_user",
    val dayOfWeek: String = "MON", // "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
    val startTime: String = "07:00 PM",
    val endTime: String = "08:00 PM",
    val durationMinutes: Int = 60,
    val subject: String = "Mathematics",
    val topic: String = "",
    val isAutoFocus: Boolean = false,
    val isStrictMode: Boolean = false,
    val blockedAppsCount: Int = 4,
    val repeatType: String = "DAILY", // ONCE, DAILY, WEEKLY, SELECTED_DAYS
    val repeatDaysJson: String = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\",\"SAT\",\"SUN\"]",
    val reminderMinutesBefore: Int = 15, // 0: Off, 5, 10, 15, 30
    val isPaused: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_schedule_logs")
data class StudyScheduleLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "current_user",
    val scheduleId: String = "",
    val subject: String = "Mathematics",
    val topic: String = "",
    val scheduledDateMillis: Long = System.currentTimeMillis(),
    val scheduledStartTime: String = "07:00 PM",
    val plannedMinutes: Int = 60,
    val actualMinutesSpent: Int = 0,
    val status: String = "PLANNED", // "PLANNED", "COMPLETED", "MISSED", "RESCHEDULED", "SKIPPED", "INTERRUPTED"
    val rescheduledToMillis: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class WeeklyScheduleAnalytics(
    val plannedMinutes: Int = 0,
    val completedMinutes: Int = 0,
    val missedSessionsCount: Int = 0,
    val completionPercentage: Int = 0,
    val subjectWiseBreakdown: Map<String, SubjectScheduleStats> = emptyMap()
)

data class SubjectScheduleStats(
    val plannedMinutes: Int = 0,
    val completedMinutes: Int = 0,
    val completionPercentage: Int = 0
)
