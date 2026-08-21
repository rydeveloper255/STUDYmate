package com.example.service.intelligence

import com.example.data.model.MockTestType

data class PerformanceCategory(
    val level: String, // Needs Practice, Improving, Good, Strong, Excellent
    val description: String,
    val emoji: String
)

data class SubjectPerformance(
    val subject: String,
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val accuracyPercent: Float,
    val score: Float,
    val timeSpentSeconds: Int,
    val avgTimePerQuestionSeconds: Float
)

data class ChapterPerformance(
    val chapter: String,
    val topic: String,
    val subject: String,
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val accuracyPercent: Float,
    val hasSufficientData: Boolean // True if totalQuestions >= 2
)

data class TimeAccuracyPatternMatrix(
    val highTimeWrongCount: Int,
    val lowTimeWrongCount: Int,
    val highTimeCorrectCount: Int,
    val lowTimeCorrectCount: Int,
    val fastestQuestionIndex: Int,
    val fastestTimeSeconds: Int,
    val longestQuestionIndex: Int,
    val longestTimeSeconds: Int,
    val neutralAdvice: String
)

data class NovaPostTestInsight(
    val whatWentWell: List<String>,
    val whatNeedsPractice: List<String>,
    val recommendedNextStep: String,
    val language: String = "English",
    val isAiGenerated: Boolean = false
)

data class SmartPracticeRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val priority: Int,
    val targetExam: String,
    val targetSubject: String,
    val targetChapter: String,
    val targetTopic: String,
    val recommendedQuestionCount: Int,
    val recommendedDifficulty: String,
    val recommendedType: MockTestType
)

data class TestIntelligenceResult(
    val examName: String,
    val title: String,
    val testType: MockTestType,
    val score: Float,
    val totalQuestions: Int,
    val accuracyPercent: Float,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val timeSpentSeconds: Int,
    val avgTimePerQuestionSeconds: Float,
    val performanceCategory: PerformanceCategory,
    val subjectPerformances: List<SubjectPerformance>,
    val chapterPerformances: List<ChapterPerformance>,
    val weakTopics: List<String>,
    val strongTopics: List<String>,
    val timeMatrix: TimeAccuracyPatternMatrix,
    val novaInsight: NovaPostTestInsight,
    val recommendations: List<SmartPracticeRecommendation>
)
