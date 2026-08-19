package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Single authoritative Selected Exam model representing CURRENT_SELECTED_EXAM.
 */
data class SelectedExam(
    val examId: String,
    val examName: String,
    val examCategory: String,
    val targetDateMillis: Long = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000,
    val targetScore: String = "Top 500 Rank / 99%ile",
    val priority: String = "HIGH",
    val status: String = "ACTIVE",
    val examPattern: String = "",
    val totalMarks: Int = 100,
    val durationMinutes: Int = 90,
    val isCustom: Boolean = false
) {
    val daysRemaining: Int
        get() {
            val diff = targetDateMillis - System.currentTimeMillis()
            return (diff / (1000L * 60 * 60 * 24)).coerceAtLeast(1).toInt()
        }

    companion object {
        fun defaultExam(): SelectedExam {
            return SelectedExam(
                examId = "railway_rrb_ntpc",
                examName = "RRB NTPC (Railway Recruitment Board)",
                examCategory = "Railway Exams",
                targetDateMillis = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000,
                targetScore = "85+ Marks (Top Merit)",
                priority = "HIGH",
                status = "ACTIVE",
                examPattern = "CBT-1: General Awareness (40 Qs), Mathematics (30 Qs), Reasoning (30 Qs) = 100 Marks (90 mins)",
                totalMarks = 100,
                durationMinutes = 90,
                isCustom = false
            )
        }
    }
}

/**
 * Definition of a supported Exam in the catalog.
 */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val shortCode: String,
    val description: String,
    val examPattern: String,
    val totalMarks: Int = 100,
    val durationMinutes: Int = 90,
    val conductsConductingBody: String = "Official Board",
    val officialWebsite: String = "",
    val isCustom: Boolean = false,
    val isPopular: Boolean = false,
    val iconName: String = "School"
)

/**
 * Subject mapped to an Exam.
 */
@Entity(
    tableName = "exam_subjects",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examId"])]
)
data class ExamSubjectEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val name: String,
    val code: String = "",
    val isOfficial: Boolean = true,
    val weightagePercent: Int = 25,
    val totalChaptersCount: Int = 10,
    val totalTopicsCount: Int = 40,
    val iconName: String = "Book",
    val colorHex: String = "#4A90E2"
)

/**
 * Chapter mapped to a Subject and Exam.
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = ExamSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"]), Index(value = ["examId"])]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val examId: String,
    val name: String,
    val orderIndex: Int = 0,
    val description: String = "",
    val isHighYield: Boolean = false
)

/**
 * Topic mapped to a Chapter, Subject, and Exam.
 */
@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapterId"]), Index(value = ["subjectId"]), Index(value = ["examId"])]
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val subjectId: String,
    val examId: String,
    val name: String,
    val isHighYield: Boolean = false,
    val estimatedStudyMinutes: Int = 30,
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val keyFormulasCount: Int = 0,
    val isOfficial: Boolean = true,
    val orderIndex: Int = 0
)

/**
 * Rich hierarchy data transfer object for UI rendering.
 */
data class ExamHierarchy(
    val exam: ExamEntity,
    val subjects: List<ExamSubjectHierarchy>
)

data class ExamSubjectHierarchy(
    val subject: ExamSubjectEntity,
    val chapters: List<ChapterWithTopics>
)

data class ChapterWithTopics(
    val chapter: ChapterEntity,
    val topics: List<TopicEntity>
)
