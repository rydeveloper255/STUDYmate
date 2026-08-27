package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Step 57: Exam Goal Model representing an active or archived exam target.
 */
@Entity(tableName = "exam_goals")
data class ExamGoalEntity(
    @PrimaryKey val examId: String, // e.g. "ssc_cgl_2026", "rrb_ntpc", or custom UUID
    val userId: String = "current_user",
    val examName: String,
    val organization: String = "", // e.g. "SSC", "RRB", "UPSC", "NTA"
    val examDateMillis: Long? = null, // Nullable epoch millis for exam date
    val isExamDateKnown: Boolean = true,
    val target: String = "Top Merit / High Score",
    val priority: String = "PRIMARY", // PRIMARY, SECONDARY, LOW
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, ARCHIVED, DATE_PASSED
    val subjectsJson: String = "[]", // List<String> serialized as JSON array
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDaysRemaining(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int? {
        if (!isExamDateKnown || examDateMillis == null || examDateMillis == 0L) return null
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val examDate = Instant.ofEpochMilli(examDateMillis).atZone(zoneId).toLocalDate()
        if (examDate.isBefore(today)) return -1 // Passed
        return java.time.temporal.ChronoUnit.DAYS.between(today, examDate).toInt()
    }
}

/**
 * Step 57: Structured Syllabus & Topic Progress Entity
 */
@Entity(tableName = "syllabus_topics")
data class SyllabusTopicEntity(
    @PrimaryKey val topicId: String, // Unique ID e.g. "ssc_math_percentage"
    val examId: String,
    val subjectName: String, // e.g. "Quantitative Aptitude"
    val topicName: String, // e.g. "Percentage"
    val subtopicName: String = "", // Optional subtopic
    val orderIndex: Int = 0,
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED, REVIEW_REQUIRED
    val studyTimeMinutes: Int = 0,
    val sessionsCount: Int = 0,
    val lastStudiedAt: Long = 0L,
    val revisionCount: Int = 0,
    val revisionStatus: String = "REVISION_NONE", // REVISION_NONE, REVISION_PENDING, REVISION_SCHEDULED, REVISION_COMPLETED
    val nextRevisionDueMillis: Long = 0L,
    val isCustom: Boolean = false,
    val isHighYield: Boolean = false
)

/**
 * DTO for Subject Syllabus Progress
 */
data class SubjectSyllabusProgress(
    val subjectName: String,
    val totalTopicsCount: Int,
    val completedTopicsCount: Int,
    val inProgressTopicsCount: Int,
    val notStartedTopicsCount: Int,
    val reviewRequiredTopicsCount: Int,
    val totalStudyMinutes: Int
) {
    val coveragePercentage: Int
        get() = if (totalTopicsCount > 0) ((completedTopicsCount.toFloat() / totalTopicsCount) * 100).toInt() else 0
}

/**
 * Overall Exam Preparation Summary DTO
 */
data class ExamPreparationSummary(
    val examGoal: ExamGoalEntity,
    val daysRemaining: Int?,
    val isDatePassed: Boolean,
    val totalTopicsCount: Int,
    val completedTopicsCount: Int,
    val inProgressTopicsCount: Int,
    val syllabusCoveragePercentage: Int,
    val subjectProgressList: List<SubjectSyllabusProgress>,
    val pendingRevisionTopicsCount: Int,
    val totalStudyMinutes: Int
)

/**
 * Generated Daily Study Plan Preview
 */
data class DailyStudyPlanPreview(
    val examId: String,
    val examName: String,
    val targetDateFormatted: String,
    val availableHoursPerDay: Float,
    val plannedItems: List<ProposedPlanItem>,
    val scheduleConflicts: List<PlanScheduleConflict>,
    val requiresUserConfirmation: Boolean = true
)

data class ProposedPlanItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val examId: String,
    val subjectName: String,
    val topicName: String,
    val sessionType: String, // "LEARNING", "PRACTICE", "REVISION"
    val targetMinutes: Int,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val priority: String = "HIGH",
    val rationale: String = ""
)

data class PlanScheduleConflict(
    val proposedItem: ProposedPlanItem,
    val existingScheduleSubject: String,
    val existingScheduleTime: String,
    val message: String
)
