package com.example.service.intelligence

import com.example.data.model.NovaActionRiskLevel
import com.example.data.model.NovaActionType
import com.example.data.model.NovaPendingSensitiveAction
import com.example.data.model.NovaToolDefinition
import org.json.JSONObject

/**
 * Step 54: Nova Strict Tool Registry & Allowlist
 * Only explicitly registered tools can be invoked by Nova.
 * Separates Safe actions from Sensitive/destructive actions that require user confirmation.
 */
object NovaToolRegistry {

    private val tools = mapOf(
        // --- SAFE ACTIONS ---
        NovaActionType.START_FOCUS to NovaToolDefinition(
            actionType = NovaActionType.START_FOCUS,
            name = "start_focus_session",
            description = "Starts a timed deep-focus study session with active Focus Shield blocking.",
            riskLevel = NovaActionRiskLevel.SAFE,
            requiredParameters = listOf("minutes")
        ),
        NovaActionType.START_STUDY_SESSION to NovaToolDefinition(
            actionType = NovaActionType.START_STUDY_SESSION,
            name = "start_study_session",
            description = "Launches a dedicated subject/topic study timer.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.START_QUIZ to NovaToolDefinition(
            actionType = NovaActionType.START_QUIZ,
            name = "start_interactive_quiz",
            description = "Opens or begins an interactive practice MCQ quiz on a topic.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.CREATE_STUDY_TASK to NovaToolDefinition(
            actionType = NovaActionType.CREATE_STUDY_TASK,
            name = "create_study_task",
            description = "Appends a new study goal or task item to today's study planner.",
            riskLevel = NovaActionRiskLevel.SAFE,
            requiredParameters = listOf("subject", "topic")
        ),
        NovaActionType.CREATE_PLAN to NovaToolDefinition(
            actionType = NovaActionType.CREATE_PLAN,
            name = "open_study_planner",
            description = "Navigates to the interactive Study Planner schedule.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.CREATE_REMINDER to NovaToolDefinition(
            actionType = NovaActionType.CREATE_REMINDER,
            name = "create_reminder",
            description = "Sets a local push reminder for upcoming study sessions.",
            riskLevel = NovaActionRiskLevel.SAFE,
            requiredParameters = listOf("title", "time")
        ),
        NovaActionType.OPEN_MOCK_TEST to NovaToolDefinition(
            actionType = NovaActionType.OPEN_MOCK_TEST,
            name = "open_mock_test",
            description = "Opens the Full CBT Mock Test screen for the selected exam.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_CURRENT_AFFAIRS to NovaToolDefinition(
            actionType = NovaActionType.OPEN_CURRENT_AFFAIRS,
            name = "open_current_affairs",
            description = "Opens daily curated exam-relevant Current Affairs & Samayiki.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_VACANCIES to NovaToolDefinition(
            actionType = NovaActionType.OPEN_VACANCIES,
            name = "open_smart_vacancies",
            description = "Navigates to Verified Sarkari Vacancies & Job notifications.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_RESULTS_HUB to NovaToolDefinition(
            actionType = NovaActionType.OPEN_RESULTS_HUB,
            name = "open_results_hub",
            description = "Opens Sarkari exam results and cutoff scorecards.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_ADMIT_CARDS to NovaToolDefinition(
            actionType = NovaActionType.OPEN_ADMIT_CARDS,
            name = "open_admit_cards",
            description = "Opens official Admit Card and City Intimation download links.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_SMART_NOTES to NovaToolDefinition(
            actionType = NovaActionType.OPEN_SMART_NOTES,
            name = "open_smart_notes",
            description = "Opens saved student study notes, formula sheets, and summaries.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_SMART_SEARCH to NovaToolDefinition(
            actionType = NovaActionType.OPEN_SMART_SEARCH,
            name = "open_smart_search",
            description = "Opens Academic Smart Search engine for formulas and syllabus.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.SAVE_NOTE to NovaToolDefinition(
            actionType = NovaActionType.SAVE_NOTE,
            name = "save_study_note",
            description = "Saves the current explanation as a structured Smart Note in Room database.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.ADD_TOPIC_TO_REVISION to NovaToolDefinition(
            actionType = NovaActionType.ADD_TOPIC_TO_REVISION,
            name = "add_topic_to_revision",
            description = "Adds a concept card to the Spaced Repetition revision queue.",
            riskLevel = NovaActionRiskLevel.SAFE,
            requiredParameters = listOf("topic")
        ),
        NovaActionType.WHY_STUDY_THIS to NovaToolDefinition(
            actionType = NovaActionType.WHY_STUDY_THIS,
            name = "why_study_this",
            description = "Analyzes past exam shift weightage and justifies study priority.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.EXPLAIN_NEWS to NovaToolDefinition(
            actionType = NovaActionType.EXPLAIN_NEWS,
            name = "explain_exam_news",
            description = "Explains official notifications and news developments with exam takeaways.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.VERIFY_FACT to NovaToolDefinition(
            actionType = NovaActionType.VERIFY_FACT,
            name = "verify_fact",
            description = "Cross-references academic or exam claims across verified official sources.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.SHOW_PROGRESS to NovaToolDefinition(
            actionType = NovaActionType.SHOW_PROGRESS,
            name = "show_study_progress",
            description = "Displays student consistency stats, study streak, and analytics.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_LIVE_EXAM_INTELLIGENCE to NovaToolDefinition(
            actionType = NovaActionType.OPEN_LIVE_EXAM_INTELLIGENCE,
            name = "open_live_exam_intelligence",
            description = "Opens the Live Exam Radar and verified board notices feed.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_MEMORY to NovaToolDefinition(
            actionType = NovaActionType.OPEN_MEMORY,
            name = "open_memory_center",
            description = "Opens student preferences and personalized context memory settings.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_SETTINGS to NovaToolDefinition(
            actionType = NovaActionType.OPEN_SETTINGS,
            name = "open_nova_settings",
            description = "Opens Nova voice, personality, and privacy settings.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.OPEN_NOTIFICATIONS to NovaToolDefinition(
            actionType = NovaActionType.OPEN_NOTIFICATIONS,
            name = "open_notification_center",
            description = "Opens the in-app Notification Center with verified alerts and updates.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),
        NovaActionType.UPDATE_NOTIFICATION_SETTINGS to NovaToolDefinition(
            actionType = NovaActionType.UPDATE_NOTIFICATION_SETTINGS,
            name = "update_notification_settings",
            description = "Updates user alert preferences such as muting specific categories or toggling reminders.",
            riskLevel = NovaActionRiskLevel.SAFE
        ),

        // --- SENSITIVE ACTIONS (User Confirmation Required) ---
        NovaActionType.CONFIRM_HIGH_RISK_ACTION to NovaToolDefinition(
            actionType = NovaActionType.CONFIRM_HIGH_RISK_ACTION,
            name = "confirm_high_risk_action",
            description = "Generic high-risk confirmation gate for data updates.",
            riskLevel = NovaActionRiskLevel.SENSITIVE,
            confirmationTitle = "Confirm Action",
            confirmationMessage = "Are you sure you want to proceed with this modification?"
        )
    )

    /**
     * Checks if an action is supported in the strict allowlist.
     */
    fun isActionAllowed(actionType: NovaActionType): Boolean {
        if (actionType == NovaActionType.NONE) return true
        return tools.containsKey(actionType)
    }

    /**
     * Returns tool definition or null if unsupported.
     */
    fun getToolDefinition(actionType: NovaActionType): NovaToolDefinition? {
        return tools[actionType]
    }

    /**
     * Identifies if an action is sensitive and builds a confirmation request.
     */
    fun checkSensitiveAction(actionType: NovaActionType, payload: String?): NovaPendingSensitiveAction? {
        val def = tools[actionType] ?: return null
        if (def.riskLevel == NovaActionRiskLevel.SENSITIVE) {
            return NovaPendingSensitiveAction(
                actionType = actionType,
                title = def.confirmationTitle ?: "Confirm Sensitive Action",
                description = def.confirmationMessage ?: "Please confirm before Nova executes this change.",
                payload = payload
            )
        }
        return null
    }

    /**
     * Generates compact tool protocol instructions for Gemini system prompt.
     */
    fun generateAllowedToolsPrompt(): String {
        val sb = StringBuilder()
        sb.append("STRICT ALLOWLIST OF SUPPORTED ACTIONS (DO NOT INVOKE ANY OTHER ACTIONS):\n")
        tools.values.filter { it.riskLevel == NovaActionRiskLevel.SAFE }.take(12).forEach { tool ->
            sb.append("- [ACTION:${tool.actionType.name}:${tool.name}]: ${tool.description}\n")
        }
        return sb.toString()
    }
}
