package com.example.service.intelligence

import com.example.data.local.StudyMateDatabase
import com.example.data.model.MistakeItem
import com.example.data.model.MistakePatternInsight
import com.example.data.remote.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Service managing mistake notebook entries, pattern detection, and Gemini classification.
 */
class MistakeIntelligenceService(
    private val database: StudyMateDatabase,
    private val geminiRepository: GeminiRepository? = null
) {
    private val mistakeDao = database.mistakeDao()

    fun getMistakesForExam(examId: String): Flow<List<MistakeItem>> {
        return mistakeDao.getMistakesByExam(examId)
    }

    suspend fun recordMistake(
        examId: String,
        subjectId: String = "",
        chapterId: String = "",
        topicId: String = "",
        subject: String,
        chapter: String = "",
        topic: String,
        questionText: String,
        studentAnswer: String,
        correctAnswer: String,
        mistakeCategory: String = "Conceptual",
        explanation: String
    ): Long = withContext(Dispatchers.IO) {
        val cat = normalizeCategory(mistakeCategory)
        val item = MistakeItem(
            examId = examId,
            subjectId = subjectId,
            chapterId = chapterId,
            topicId = topicId,
            subject = subject,
            chapter = chapter,
            topic = topic,
            questionText = questionText,
            studentAnswer = studentAnswer,
            correctAnswer = correctAnswer,
            mistakeCategory = cat,
            explanation = explanation,
            timestamp = System.currentTimeMillis(),
            isMastered = false,
            aiClassified = false
        )
        mistakeDao.insertMistake(item)
    }

    suspend fun markMistakeMastered(id: Long, isMastered: Boolean) = withContext(Dispatchers.IO) {
        mistakeDao.updateMastered(id, isMastered)
    }

    suspend fun clearMistakesForExam(examId: String) = withContext(Dispatchers.IO) {
        mistakeDao.clearMistakesForExam(examId)
    }

    /**
     * Detects mistake patterns across a user's unmastered mistake list.
     */
    fun detectMistakePatterns(mistakes: List<MistakeItem>): List<MistakePatternInsight> {
        val unmastered = mistakes.filter { !it.isMastered }
        if (unmastered.isEmpty()) return emptyList()

        val insights = mutableListOf<MistakePatternInsight>()

        // Group by category and subject
        val byCategory = unmastered.groupBy { it.mistakeCategory }
        val byTopic = unmastered.groupBy { "${it.subject} - ${it.topic}" }

        for ((cat, items) in byCategory) {
            if (items.size >= 2) {
                val sub = items.first().subject
                val advice = when (cat) {
                    "Calculation" -> "Slow down during multi-step numerical steps. Write out intermediate formulas."
                    "Conceptual" -> "Review foundational principles in $sub before attempting speed practice."
                    "Careless" -> "Re-read question constraints (e.g. units, NOT true statements) before submitting."
                    "Time Pressure" -> "Practice timed sub-topic drills (1 min per question) to build confidence under time constraints."
                    else -> "Analyze correct step-by-step solutions carefully."
                }
                insights.add(
                    MistakePatternInsight(
                        patternType = cat,
                        iconName = "⚠️",
                        title = "Repeated $cat Mistakes",
                        description = "You've made $cat errors ${items.size} times in $sub.",
                        affectedSubject = sub,
                        frequency = items.size,
                        targetedAdvice = advice
                    )
                )
            }
        }

        for ((topicKey, items) in byTopic) {
            if (items.size >= 3) {
                val sub = items.first().subject
                val top = items.first().topic
                insights.add(
                    MistakePatternInsight(
                        patternType = "Topic Weakness",
                        iconName = "🎯",
                        title = "Struggling in $top",
                        description = "${items.size} unmastered mistakes accumulated in $top.",
                        affectedSubject = sub,
                        frequency = items.size,
                        targetedAdvice = "Schedule a dedicated 25-minute concept review for $top."
                    )
                )
            }
        }

        return insights.sortedByDescending { it.frequency }.take(5)
    }

    private fun normalizeCategory(category: String): String {
        val lower = category.lowercase()
        return when {
            lower.contains("calc") || lower.contains("math") -> "Calculation"
            lower.contains("concept") || lower.contains("theory") -> "Conceptual"
            lower.contains("careless") || lower.contains("silly") -> "Careless"
            lower.contains("time") || lower.contains("speed") -> "Time Pressure"
            lower.contains("guess") -> "Guess"
            else -> "Conceptual"
        }
    }
}
