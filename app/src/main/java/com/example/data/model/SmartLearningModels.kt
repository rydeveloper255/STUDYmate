package com.example.data.model

import java.io.Serializable

// =============================================================================
// STEP 23 SMART LEARNING SYSTEM MODELS
// =============================================================================

/**
 * Source Trust Quality Classification (Step 23 Feature 5)
 */
enum class SourceTrustLevel(
    val label: String,
    val badgeText: String,
    val icon: String,
    val priority: Int
) {
    OFFICIAL("Official", "🟢 Official Source", "🏛️", 1),
    REPUTABLE("Reliable Source", "🔵 Reliable Source", "📰", 2),
    EDUCATIONAL("Reference Source", "🟡 Reference Source", "📚", 3),
    UNVERIFIED("Needs Verification", "⚪ Needs Verification", "❓", 4)
}

/**
 * Consistency Signals across independent sources (Step 23 Feature 5)
 */
enum class SourceConsistencySignal(
    val displayText: String,
    val icon: String
) {
    CONFIRMED_MULTIPLE("✓ Confirmed by multiple sources", "✓"),
    LIMITED_CONFIRMATION("⚠ Limited source confirmation", "⚠"),
    SOURCES_DIFFER("⚠ Sources differ", "⚠️")
}

/**
 * Detailed Source Quality Record
 */
data class SourceQualityRecord(
    val url: String,
    val domain: String,
    val name: String,
    val title: String = "",
    val trustLevel: SourceTrustLevel = SourceTrustLevel.REPUTABLE,
    val trustLabel: String = "Reliable Source",
    val consistencySignal: SourceConsistencySignal = SourceConsistencySignal.CONFIRMED_MULTIPLE,
    val isOfficial: Boolean = false,
    val publicationDate: String? = null,
    val retrievedAt: Long = System.currentTimeMillis(),
    val disagreementNotes: String? = null
) : Serializable

/**
 * Configuration for Web -> Fresh MCQ Generation (Step 23 Feature 1 & 2)
 */
data class SmartMcqConfig(
    val questionCount: Int = 10, // 5, 10, 20, 30
    val difficulty: String = "Mixed", // "Easy", "Medium", "Hard", "Mixed"
    val language: String = "English", // "English", "Hindi"
    val questionType: String = "MCQ", // "MCQ", "True/False", "Mixed"
    val examName: String = "Competitive Exam",
    val subject: String = "General Awareness",
    val topicQuery: String = ""
) : Serializable

/**
 * Validation Outcome for AI Generated Questions (Step 23 Feature 5)
 */
data class QuestionValidationResult(
    val isValid: Boolean,
    val validationSummary: String,
    val passedCount: Int,
    val discardedCount: Int,
    val questions: List<Question>
)

/**
 * Generated MCQ Batch with full lineage and source references
 */
data class GeneratedMcqBatch(
    val id: String = java.util.UUID.randomUUID().toString(),
    val topic: String,
    val examName: String,
    val subject: String,
    val config: SmartMcqConfig,
    val questions: List<Question>,
    val sourceReferences: List<WebSearchSource>,
    val generatedAt: Long = System.currentTimeMillis(),
    val isValidated: Boolean = true,
    val saveStatus: String = "✓ Saved" // "Saving...", "✓ Saved", "⚠ Unable to save"
) : Serializable

/**
 * Spaced Revision States (Step 23 Feature 3 & 4)
 */
enum class SpacedRevisionState(
    val label: String,
    val badgeIcon: String
) {
    NEW("New", "🌱"),
    REVIEW_SOON("Review Soon", "⏳"),
    DUE("Due", "🔴"),
    RECENTLY_REVISED("Recently Revised", "✓")
}

/**
 * Smart Revision Priority Tiers (Step 23 Feature 3)
 */
enum class SmartRevisionPriority(
    val label: String,
    val order: Int
) {
    WEAK_TOPIC("🔴 Weak Topic", 1),
    CURRENT_AFFAIRS_DUE("🟡 Current Affairs Due", 2),
    RECENTLY_SAVED("🟢 Recently Saved Topic", 3),
    REGULAR("Standard Topic", 4)
}

/**
 * Detailed Smart Revision Item for Multi-Step Learning
 */
data class SmartRevisionTopicItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val contentId: String = "",
    val title: String,
    val subject: String,
    val topic: String,
    val recapSummary: String,
    val importantFacts: List<String> = emptyList(),
    val whyItMatters: String = "",
    val examRelevanceLevel: String = "HIGH", // HIGH, MEDIUM, LOW
    val sourceName: String = "Educational Reference",
    val sourceUrl: String = "",
    val trustLevel: SourceTrustLevel = SourceTrustLevel.REPUTABLE,
    val revisionState: SpacedRevisionState = SpacedRevisionState.DUE,
    val priority: SmartRevisionPriority = SmartRevisionPriority.CURRENT_AFFAIRS_DUE,
    val priorityReason: String = "",
    val lastReviewedAt: Long = 0L,
    val nextReviewAt: Long = System.currentTimeMillis(),
    val mistakeCount: Int = 0,
    val miniQuizQuestions: List<Question> = emptyList(),
    val isSaved: Boolean = true
) : Serializable

/**
 * 6-Step Smart Revision Session State (Step 23 Feature 3)
 */
data class SmartRevisionSessionState(
    val currentStep: Int = 1, // 1: Recap, 2: Important Facts, 3: Why it matters, 4: Short MCQs, 5: Mistake Review, 6: Status
    val totalSteps: Int = 6,
    val item: SmartRevisionTopicItem? = null,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isAnswerSubmitted: Map<Int, Boolean> = emptyMap(),
    val reviewedMistakes: Set<Int> = emptySet(),
    val isCompleted: Boolean = false,
    val saveStatus: String = "" // "Saving...", "✓ Saved", "⚠ Unable to save"
)

/**
 * Daily Exam Briefing Data (Step 23 Feature 4)
 */
data class DailyExamBriefing(
    val id: String = java.util.UUID.randomUUID().toString(),
    val examName: String = "RRB Group D",
    val dateFormatted: String = "",
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val examUpdates: List<LiveExamUpdateEntity> = emptyList(),
    val topCurrentAffairs: List<CurrentAffairsItem> = emptyList(),
    val officialNotice: LiveExamUpdateEntity? = null,
    val trendingTopic: TrendingExamTopicEntity? = null,
    val studyPriorityTopic: String = "Core General Awareness",
    val prioritySubject: String = "General Studies",
    val priorityRationale: String = "High recurring weightage in upcoming exam shift.",
    val miniQuiz: List<Question> = emptyList(),
    val isLive: Boolean = true,
    val statusMessage: String = "✓ Up to date"
) : Serializable

/**
 * Source Trust Verification Outcome (Step 23 Feature 5)
 */
data class SourceTrustVerification(
    val sourceUrl: String,
    val domain: String,
    val trustLevel: SourceTrustLevel,
    val trustBadge: String,
    val consistencySignal: SourceConsistencySignal,
    val explanation: String,
    val isOfficial: Boolean,
    val crossReferenceSources: List<WebSearchSource> = emptyList(),
    val conflicts: List<String> = emptyList()
) : Serializable
