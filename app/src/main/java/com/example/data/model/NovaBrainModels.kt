package com.example.data.model

/**
 * Step 54: Nova AI Brain + Context + Action System Models
 * Enforces structured intent classification, privacy-conscious compact context,
 * strict action allowlists with safe vs. sensitive classification, and authentic Hinglish mentoring.
 */

enum class NovaIntent(val displayName: String, val isLocalFastPath: Boolean) {
    STUDY_STATUS("Study Status & Progress", true),
    START_FOCUS("Start Focus Session", true),
    PLAN_DAY("Daily Study Planning", true),
    MOCK_TEST("CBT Mock Test", true),
    PRACTICE_QUIZ("Practice Quiz / MCQs", false),
    SOLVE_DOUBT("Academic Concept & Doubt", false),
    EXPLAIN_EASIER("Simplify / ELI5 Explanation", false),
    CURRENT_AFFAIRS("Current Affairs & Samayiki", true),
    SMART_SEARCH("Academic & Web Search", false),
    FACT_CHECK("Fact Verification", false),
    WHY_STUDY("Exam Weightage & Why Study This", false),
    RECRUITMENT_INFO("Vacancies, Results & Admit Cards", true),
    REVISION_DUE("Spaced Revision & Weak Topics", true),
    REMEMBER_PREFERENCE("Save Personal Preference", true),
    NOTIFICATION_INFO("Recent Notifications & Alerts", true),
    NOTIFICATION_SETTINGS("Update Notification Preferences", true),
    SENSITIVE_ACTION_REQUEST("Destructive / Sensitive Modification", false),
    GENERAL_CHAT("Conversational Mentoring", false)
}

enum class NovaActionRiskLevel {
    SAFE,       // Instant 1-tap or automated trigger
    SENSITIVE   // Requires explicit student confirmation dialog before execution
}

data class NovaToolDefinition(
    val actionType: NovaActionType,
    val name: String,
    val description: String,
    val riskLevel: NovaActionRiskLevel = NovaActionRiskLevel.SAFE,
    val requiredParameters: List<String> = emptyList(),
    val confirmationTitle: String? = null,
    val confirmationMessage: String? = null
)

data class NovaIntentClassificationResult(
    val intent: NovaIntent,
    val confidence: Float = 1.0f,
    val extractedSubject: String? = null,
    val extractedTopic: String? = null,
    val extractedMinutes: Int? = null,
    val extractedExam: String? = null,
    val extractedQuery: String? = null,
    val proposedAction: NovaActionType = NovaActionType.NONE,
    val actionPayload: String? = null,
    val isDeterministicFastPath: Boolean = false
)

data class NovaCompactContext(
    val studentName: String = "Scholar",
    val targetExam: String = "Competitive Exam",
    val examDaysRemaining: Int = 30,
    val preferredLanguage: String = "Hinglish",
    val activeScreen: String = "Chat",
    // Verified Real State (Zero Hallucination)
    val todayFocusMinutes: Int = 0,
    val dailyTargetMinutes: Int = 180,
    val currentStreak: Int = 0,
    val completedTasksToday: Int = 0,
    val pendingTasksCount: Int = 0,
    val pendingTasksPreview: List<String> = emptyList(),
    val revisionsDueCount: Int = 0,
    val revisionsDuePreview: List<String> = emptyList(),
    val topWeakTopics: List<String> = emptyList(),
    val topStrongTopics: List<String> = emptyList(),
    val recentMockAccuracy: Float? = null,
    val recentMockName: String? = null,
    val memoriesSummary: List<String> = emptyList()
)

data class NovaPendingSensitiveAction(
    val actionType: NovaActionType,
    val title: String,
    val description: String,
    val payload: String? = null,
    val onConfirmCallbackKey: String = ""
)
