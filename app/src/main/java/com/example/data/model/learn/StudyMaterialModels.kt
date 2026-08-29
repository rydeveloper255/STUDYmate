package com.example.data.model.learn

import java.io.Serializable
import java.util.UUID

/**
 * Status of study material generation and Supabase sync.
 */
enum class MaterialSourceStatus(val value: String) {
    PENDING("pending"),
    PROCESSING("processing"),
    READY("ready"),
    FAILED("failed")
}

/**
 * Pillar 1: Master Record representing Supabase table `study_materials`.
 * Key combination: exam_name + subject_name + chapter_name.
 */
data class StudyMaterialMaster(
    val id: String = UUID.randomUUID().toString(),
    val examName: String,
    val subjectName: String,
    val chapterName: String,
    val sourceStatus: MaterialSourceStatus = MaterialSourceStatus.READY,
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val estimatedStudyTime: String = "60 mins",
    val chapterOverview: ChapterOverviewData = ChapterOverviewData(),
    val topics: List<ChapterTopicItem> = emptyList(),
    val concepts: List<ConceptLearningItem> = emptyList(),
    val solvedExamples: List<SolvedExampleItem> = emptyList(),
    val practiceQuestions: List<ChapterPracticeQuestion> = emptyList(),
    val previousYearQuestions: List<ChapterPyqQuestion> = emptyList(),
    val quickRevision: QuickRevisionData = QuickRevisionData(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable

/**
 * Chapter Overview data structure.
 */
data class ChapterOverviewData(
    val summary: String = "",
    val whatThisChapterCovers: List<String> = emptyList(),
    val importantConcepts: List<String> = emptyList(),
    val examRelevance: String = "High",
    val weightagePercent: Int = 15,
    val prerequisites: List<String> = emptyList()
) : Serializable

/**
 * Topic / Subtopic item with progress tracking.
 */
data class ChapterTopicItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val orderIndex: Int = 0,
    val estimatedMinutes: Int = 20,
    val status: String = "NOT_STARTED", // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    val isHighYield: Boolean = false,
    val keyPoints: List<String> = emptyList()
) : Serializable

/**
 * Concept Learning data structure.
 */
data class ConceptLearningItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val simpleExplanation: String,
    val detailedExplanation: String,
    val realWorldAnalogy: String = "",
    val examples: List<String> = emptyList(),
    val proTips: List<String> = emptyList(),
    val keyFormulas: List<String> = emptyList(),
    val isCompleted: Boolean = false
) : Serializable

/**
 * Solved Example with step-by-step solution.
 */
data class SolvedExampleItem(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val difficulty: String = "Medium",
    val stepByStepSolution: List<String> = emptyList(),
    val finalAnswer: String = "",
    val explanation: String = "",
    val commonPitfall: String = ""
) : Serializable

/**
 * Chapter Practice Question (MCQ / Numerical).
 */
data class ChapterPracticeQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val explanation: String = "",
    val difficulty: String = "Medium",
    val examCategory: String = "",
    val userSelectedOption: Int? = null,
    val isAnswered: Boolean = false
) : Serializable

/**
 * Previous Year Question (PYQ).
 */
data class ChapterPyqQuestion(
    val id: String = UUID.randomUUID().toString(),
    val examName: String = "",
    val examYear: String = "2023",
    val shift: String = "Shift 1",
    val question: String,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val detailedSolution: String = ""
) : Serializable

/**
 * Quick Revision "Chapter in 5 Minutes".
 */
data class QuickRevisionData(
    val fiveMinuteRecap: String = "",
    val keyConcepts: List<String> = emptyList(),
    val essentialFormulas: List<String> = emptyList(),
    val importantFacts: List<String> = emptyList(),
    val commonMistakes: List<String> = emptyList()
) : Serializable

/**
 * Pillar 2: Supabase table `study_formulas`.
 * Formula Sheet representation with variable meanings, when to use, examples, and importance.
 */
data class StudyFormulaItem(
    val id: String = UUID.randomUUID().toString(),
    val materialId: String = "",
    val examName: String,
    val subjectName: String,
    val chapterName: String,
    val formulaTitle: String,
    val formula: String,
    val variableMeanings: String,
    val whenToUse: String,
    val example: String,
    val importanceLevel: String = "HIGH", // "HIGH", "CRITICAL", "MEDIUM", "LOW"
    val source: String = "Official Syllabus & Reference",
    val createdAt: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
) : Serializable

/**
 * Pillar 3: Supabase table `study_important_notes`.
 * Important Notes representation with definitions, rules, facts, short tricks, and common mistakes.
 */
data class StudyImportantNoteItem(
    val id: String = UUID.randomUUID().toString(),
    val materialId: String = "",
    val examName: String,
    val subjectName: String,
    val chapterName: String,
    val title: String,
    val content: String,
    val category: String = "Important Facts", // "Definitions", "Rules", "Important Facts", "Short Tricks", "Common Mistakes"
    val importance: String = "HIGH", // "HIGH", "CRITICAL", "MEDIUM", "LOW"
    val source: String = "NCERT & Standard Exam Syllabus",
    val createdAt: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
) : Serializable

/**
 * Chapter Progress metrics for the 10-pillar tracker.
 */
data class ChapterProgressMetrics(
    val chapterName: String,
    val conceptsTotal: Int = 0,
    val conceptsCompleted: Int = 0,
    val formulasTotal: Int = 0,
    val formulasReviewed: Int = 0,
    val notesTotal: Int = 0,
    val notesViewed: Int = 0,
    val questionsTotal: Int = 0,
    val questionsSolved: Int = 0,
    val questionsCorrect: Int = 0,
    val isBookmarked: Boolean = false
) : Serializable {
    val accuracyPercent: Int
        get() = if (questionsSolved > 0) ((questionsCorrect.toFloat() / questionsSolved) * 100).toInt() else 0

    val overallProgressPercent: Int
        get() {
            var weighted = 0f
            var totalWeight = 0f

            if (conceptsTotal > 0) {
                weighted += (conceptsCompleted.toFloat() / conceptsTotal) * 40f
                totalWeight += 40f
            }
            if (formulasTotal > 0) {
                weighted += (formulasReviewed.toFloat() / formulasTotal) * 20f
                totalWeight += 20f
            }
            if (notesTotal > 0) {
                weighted += (notesViewed.toFloat() / notesTotal) * 20f
                totalWeight += 20f
            }
            if (questionsTotal > 0) {
                weighted += (questionsSolved.toFloat() / questionsTotal) * 20f
                totalWeight += 20f
            }

            return if (totalWeight > 0f) ((weighted / totalWeight) * 100).toInt().coerceIn(0, 100) else 0
        }
}
