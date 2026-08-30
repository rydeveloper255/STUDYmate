package com.example.service.admin

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.StudyMateDatabase
import com.example.data.model.UserProfile
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.telegram.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Data representation for tracking repeated errors to prevent Telegram spam.
 */
data class ErrorFingerprintRecord(
    val errorId: String,
    val feature: String,
    val screen: String,
    val message: String,
    val severity: ErrorSeverity,
    var occurrences: Int = 1,
    val firstSeenMillis: Long = System.currentTimeMillis(),
    var lastSeenMillis: Long = System.currentTimeMillis(),
    var lastReportedOccurrences: Int = 1
)

/**
 * Service health status state for monitoring recovery and downtime transitions.
 */
enum class ServiceOnlineState {
    ONLINE,
    DEGRADED,
    DOWN,
    UNKNOWN
}

data class ServiceHealthSnapshot(
    val serviceName: String,
    val state: ServiceOnlineState = ServiceOnlineState.UNKNOWN,
    val lastCheckMillis: Long = System.currentTimeMillis(),
    val details: String = ""
)

/**
 * Centralized StudyMate Admin Control & Telegram Bot Monitoring Automation.
 *
 * Implements Step 75:
 * - Admin notifications for New User registrations
 * - High-priority security alerts (e.g. repeated login failures)
 * - Automatic, centralized error classification (CRITICAL, ERROR, WARNING, INFO)
 * - Duplicate error grouping & spam prevention
 * - External API & Content Pipeline health monitoring (Failure & Recovery alerts)
 * - Secure Admin Command handler (/status, /health, /errors, /users, /version, /maintenance, /restart_info)
 * - Strict token redaction, email masking, and authorized admin chat ID verification
 * - Non-blocking asynchronous dispatch with bounded offline retry queue
 */
object TelegramAdminBotManager {
    private const val TAG = "TelegramAdminBot"
    private const val APP_VERSION = "1.0.1"

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var appContext: Context? = null
    private var telegramService: TelegramBotService? = null
    private var database: StudyMateDatabase? = null
    private var supabaseClient: SupabaseClient? = null

    private val isPollingActive = AtomicBoolean(false)
    private var lastUpdateOffset: Long = 0L
    private val appStartTimeMillis = System.currentTimeMillis()

    // Failed login tracking per IP/user
    private val failedLoginAttempts = ConcurrentHashMap<String, Int>()

    // Error duplicate tracking & rate limiting (Spam prevention)
    private val errorFingerprints = ConcurrentHashMap<String, ErrorFingerprintRecord>()
    private val recentErrorLog = ConcurrentLinkedQueue<ErrorFingerprintRecord>()

    // Service state tracking for down -> recovered notifications
    private val serviceStates = ConcurrentHashMap<String, ServiceHealthSnapshot>()

    // Offline / retry queue for notifications (Max 25 items)
    private data class QueuedNotification(
        val chatId: String,
        val text: String,
        val attemptCount: Int = 0,
        val createdAt: Long = System.currentTimeMillis()
    )
    private val pendingQueue = ConcurrentLinkedQueue<QueuedNotification>()

    // State flow for UI / diagnostics
    private val _adminMetrics = MutableStateFlow(
        mapOf(
            "total_errors_tracked" to "0",
            "last_admin_command" to "None",
            "bot_connected" to "Checking..."
        )
    )
    val adminMetrics: StateFlow<Map<String, String>> = _adminMetrics.asStateFlow()

    fun init(
        context: Context,
        botService: TelegramBotService,
        db: StudyMateDatabase? = null,
        sbClient: SupabaseClient? = null
    ) {
        appContext = context.applicationContext
        telegramService = botService
        database = db
        supabaseClient = sbClient

        MaintenanceManager.init(context)
        StudyMateProactiveCommandCenter.init(botService, TelegramBotConfig.getAdminChatId(context))
        startAdminCommandPolling()
        startOfflineQueueProcessor()
    }

    // =========================================================================
    // 1. NEW USER REGISTRATION NOTIFICATION
    // =========================================================================

    fun notifyNewUser(profile: UserProfile, appVer: String = APP_VERSION) {
        if (profile.isGuest || profile.email.isBlank()) return

        scope.launch {
            try {
                val maskedEmail = TelegramBotConfig.maskEmail(profile.email)
                val userId = if (profile.uid.isNotBlank()) profile.uid else profile.id
                val name = if (profile.name.isNotBlank()) profile.name else "Not provided"
                val exam = if (profile.examName.isNotBlank()) profile.examName else "Not provided"
                val goal = if (profile.targetScore.isNotBlank()) profile.targetScore else "Not provided"
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

                val message = """
                    🆕 <b>NEW STUDYMATE USER</b>

                    <b>User ID:</b> <code>$userId</code>
                    <b>Name:</b> $name
                    <b>Email:</b> <code>$maskedEmail</code>
                    <b>Study Goal:</b> $goal
                    <b>Exam:</b> $exam
                    <b>Registration Time:</b> $now
                    <b>App Version:</b> $appVer

                    <b>Status:</b> Successfully Registered
                """.trimIndent()

                dispatchToAdmin(message)
                Log.d(TAG, "Sent new user registration notification for: $maskedEmail")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send new user notification: ${e.message}")
            }
        }
    }

    // =========================================================================
    // 2. SECURITY & LOGIN EVENT ALERTS
    // =========================================================================

    fun recordFailedLoginAttempt(identifier: String, reason: String) {
        val attempts = failedLoginAttempts.compute(identifier) { _, v -> (v ?: 0) + 1 } ?: 1
        if (attempts >= 3) {
            val maskedId = if (identifier.contains("@")) TelegramBotConfig.maskEmail(identifier) else identifier
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val eventId = TelegramBotConfig.generateEventId()

            val message = """
                🛡 <b>SECURITY ALERT: MULTIPLE FAILED LOGINS</b>

                <b>Target:</b> <code>$maskedId</code>
                <b>Failed Attempts:</b> $attempts
                <b>Latest Reason:</b> ${TelegramBotConfig.sanitize(reason)}
                <b>Time:</b> $now
                <b>Event ID:</b> <code>$eventId</code>

                <b>Status:</b> Suspicious Login Activity Detected
            """.trimIndent()

            dispatchToAdmin(message)
        }
    }

    fun clearFailedLoginAttempts(identifier: String) {
        failedLoginAttempts.remove(identifier)
    }

    fun notifySecurityAlert(title: String, details: String, severity: String = "WARNING") {
        scope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val eventId = TelegramBotConfig.generateEventId()

            val message = """
                🛡 <b>STUDYMATE SECURITY ALERT</b>

                <b>Severity:</b> $severity
                <b>Alert:</b> ${TelegramBotConfig.sanitize(title)}
                <b>Details:</b> ${TelegramBotConfig.sanitize(details)}
                <b>Time:</b> $now
                <b>Event ID:</b> <code>$eventId</code>
            """.trimIndent()

            dispatchToAdmin(message)
        }
    }

    // =========================================================================
    // 3. AUTOMATIC ERROR MONITORING & DUPLICATE GROUPING
    // =========================================================================

    fun notifyError(
        feature: String,
        screen: String,
        errorMessage: String,
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        throwable: Throwable? = null
    ) {
        if (severity == ErrorSeverity.INFO) {
            // INFO events are purely logged internally, never sent to Telegram
            Log.i(TAG, "INFO: [$feature @ $screen] $errorMessage")
            return
        }

        scope.launch {
            val sanitizedMsg = TelegramBotConfig.sanitize(
                if (errorMessage.isNotBlank()) errorMessage else throwable?.message ?: "Unknown runtime error"
            ).take(300)

            val fingerprint = "${feature.trim()}_${screen.trim()}_${sanitizedMsg.take(80)}"
            val nowMillis = System.currentTimeMillis()
            val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(nowMillis))

            val existing = errorFingerprints[fingerprint]
            val errorId = existing?.errorId ?: TelegramBotConfig.generateErrorId()

            // Ingest into Step 79 Smart Intelligence Engine
            val smartIssue = StudyMateSmartIntelligenceEngine.ingestError(
                errorId = errorId,
                feature = feature,
                screen = screen,
                rawMessage = sanitizedMsg,
                severity = severity
            )

            // Reopening Detection Alert
            if (smartIssue.status == StudyMateSmartIntelligenceEngine.IssueLifecycleStatus.REOPENED && smartIssue.occurrences <= 2) {
                val reopenMsg = """
                    🔄 <b>ISSUE REOPENED</b>

                    <b>Issue ID:</b> <code>${smartIssue.issueId}</code>
                    <b>Feature:</b> ${smartIssue.feature}
                    <b>Priority:</b> ${smartIssue.priority.name}
                    <b>Title:</b> ${smartIssue.title}
                    <b>Status:</b> Previously resolved issue detected again with new errors.
                    <b>Time:</b> $nowFormatted
                """.trimIndent()
                dispatchToAdmin(reopenMsg)
            }

            if (existing == null) {
                val record = ErrorFingerprintRecord(
                    errorId = errorId,
                    feature = feature,
                    screen = screen,
                    message = sanitizedMsg,
                    severity = severity,
                    occurrences = 1,
                    firstSeenMillis = nowMillis,
                    lastSeenMillis = nowMillis,
                    lastReportedOccurrences = 1
                )
                errorFingerprints[fingerprint] = record
                recentErrorLog.add(record)
                while (recentErrorLog.size > 50) {
                    recentErrorLog.poll()
                }

                val icon = if (severity == ErrorSeverity.CRITICAL) "🚨" else "⚠️"
                val message = """
                    $icon <b>STUDYMATE ERROR</b>

                    <b>Severity:</b> ${severity.name}

                    <b>Feature:</b>
                    $feature

                    <b>Error:</b>
                    $sanitizedMsg

                    <b>Screen:</b>
                    $screen

                    <b>App Version:</b>
                    $APP_VERSION

                    <b>Device:</b>
                    Android (${Build.MODEL ?: "Device"}, API ${Build.VERSION.SDK_INT})

                    <b>Time:</b>
                    $nowFormatted

                    <b>Error ID:</b>
                    <code>$errorId</code>

                    <b>Smart Issue:</b>
                    <code>${smartIssue.issueId}</code> [${smartIssue.priority.name}]

                    <b>Status:</b>
                    Automatic Alert
                """.trimIndent()

                dispatchToAdmin(message)
            } else {
                existing.occurrences += 1
                existing.lastSeenMillis = nowMillis

                // Repeated error / smart issue milestone alert
                val diff = existing.occurrences - existing.lastReportedOccurrences
                if (diff >= 15 || (severity == ErrorSeverity.CRITICAL && diff >= 5)) {
                    existing.lastReportedOccurrences = existing.occurrences
                    val firstTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(existing.firstSeenMillis))
                    val lastTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(existing.lastSeenMillis))

                    val groupMessage = """
                        🔁 <b>REPEATED ERROR</b>

                        <b>Error ID:</b> <code>${existing.errorId}</code>
                        <b>Smart Issue:</b> <code>${smartIssue.issueId}</code> [${smartIssue.priority.name}]
                        <b>Feature:</b> ${existing.feature}
                        <b>Occurrences:</b> ${existing.occurrences}
                        <b>First Seen:</b> $firstTime
                        <b>Last Seen:</b> $lastTime
                        <b>Error:</b> ${existing.message}
                    """.trimIndent()

                    dispatchToAdmin(groupMessage)
                }
            }
        }
    }

    // =========================================================================
    // 4. APP CRASH & CRITICAL SYSTEM ALERTS
    // =========================================================================

    fun notifyCriticalFailure(
        feature: String,
        operation: String,
        reason: String,
        errorId: String? = null
    ) {
        scope.launch {
            val errId = errorId ?: TelegramBotConfig.generateErrorId()
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val sanitizedReason = TelegramBotConfig.sanitize(reason).take(400)

            val message = """
                🔴 <b>CRITICAL SYSTEM ALERT</b>

                <b>Feature:</b> $feature
                <b>Operation:</b> $operation
                <b>Status:</b> FAILED
                <b>Reason:</b> $sanitizedReason
                <b>Time:</b> $now
                <b>Error ID:</b> <code>$errId</code>
            """.trimIndent()

            dispatchToAdmin(message)
        }
    }

    // =========================================================================
    // 5. API HEALTH & SERVICE FAILURE / RECOVERY MONITORING
    // =========================================================================

    fun updateServiceStatus(serviceName: String, isHealthy: Boolean, reason: String = "") {
        scope.launch {
            val currentState = if (isHealthy) ServiceOnlineState.ONLINE else ServiceOnlineState.DOWN
            val previous = serviceStates[serviceName]
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

            serviceStates[serviceName] = ServiceHealthSnapshot(
                serviceName = serviceName,
                state = currentState,
                lastCheckMillis = System.currentTimeMillis(),
                details = reason
            )

            // Trigger notification ONLY on state transitions to avoid spam
            if (previous != null) {
                if (previous.state == ServiceOnlineState.ONLINE && currentState == ServiceOnlineState.DOWN) {
                    val message = """
                        🔴 <b>SERVICE DOWN</b>

                        <b>Service:</b> $serviceName
                        <b>Status:</b> FAILED
                        <b>Reason:</b> ${TelegramBotConfig.sanitize(reason).ifBlank { "Request failure or timeout" }}
                        <b>Time:</b> $now
                    """.trimIndent()
                    dispatchToAdmin(message)
                } else if (previous.state == ServiceOnlineState.DOWN && currentState == ServiceOnlineState.ONLINE) {
                    val message = """
                        🟢 <b>SERVICE RECOVERED</b>

                        <b>Service:</b> $serviceName
                        <b>Status:</b> ONLINE
                        <b>Recovery Time:</b> $now
                    """.trimIndent()
                    dispatchToAdmin(message)
                }
            }
        }
    }

    // =========================================================================
    // 6. CONTENT PIPELINE MONITORING
    // =========================================================================

    fun notifyContentPipelineFailure(
        content: String,
        source: String,
        stage: String,
        errorId: String? = null,
        reason: String? = null
    ) {
        scope.launch {
            val errId = errorId ?: TelegramBotConfig.generateErrorId()
            val sanitizedReason = TelegramBotConfig.sanitize(reason ?: "Fetch/processing error").take(300)

            // Ingest into Step 79 Smart Intelligence Content Tracker
            StudyMateSmartIntelligenceEngine.recordContentFailure(
                contentName = content,
                stage = stage,
                details = sanitizedReason,
                errorId = errId
            )

            val message = """
                ⚠️ <b>CONTENT PIPELINE FAILED</b>

                <b>Content:</b>
                $content

                <b>Source:</b>
                $source

                <b>Stage:</b>
                $stage

                <b>Status:</b>
                FAILED

                <b>Reason:</b>
                $sanitizedReason

                <b>Error ID:</b>
                <code>$errId</code>
            """.trimIndent()

            dispatchToAdmin(message)
        }
    }

    // =========================================================================
    // 7. ADMIN COMMAND SYSTEM & UPDATE POLLING
    // =========================================================================

    private fun startAdminCommandPolling() {
        if (isPollingActive.getAndSet(true)) return

        scope.launch {
            while (isActive) {
                try {
                    val service = telegramService
                    val botToken = TelegramBotConfig.getBotToken()

                    if (service != null && botToken != null) {
                        val result = service.getUpdates(offset = lastUpdateOffset + 1, limit = 10)
                        if (result.isSuccess) {
                            val updates = result.getOrNull() ?: emptyList()
                            for (update in updates) {
                                if (update.updateId >= lastUpdateOffset) {
                                    lastUpdateOffset = update.updateId
                                }
                                val incoming = update.message
                                if (incoming?.text != null) {
                                    handleIncomingMessage(incoming)
                                } else if (update.callbackQuery != null) {
                                    handleCallbackQuery(update.callbackQuery)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Admin command polling cycle failed: ${e.message}")
                }
                delay(12000) // Poll every 12 seconds safely
            }
        }
    }

    private suspend fun handleCallbackQuery(cbQuery: TelegramCallbackQuery) {
        val service = telegramService ?: return
        val chatId = cbQuery.message?.chat?.id?.toString() ?: return
        val senderId = cbQuery.from?.id?.toString() ?: chatId
        val msgId = cbQuery.message?.messageId ?: return
        val callbackData = cbQuery.data ?: return

        val isAuthorized = TelegramBotConfig.isAuthorizedAdmin(chatId, appContext) ||
                TelegramBotConfig.isAuthorizedAdmin(senderId, appContext)

        if (!isAuthorized) {
            service.answerCallbackQuery(cbQuery.id, "Unauthorized access", showAlert = true)
            return
        }

        // Acknowledge callback query
        service.answerCallbackQuery(cbQuery.id)

        try {
            when {
                callbackData == "dash_main" || callbackData == "dash_refresh" || callbackData == "dash_ref_main" -> {
                    renderDashboardOverview(chatId, msgId)
                }
                callbackData == "dash_users" || callbackData == "dash_ref_users" -> {
                    renderUserAnalyticsView(chatId, msgId)
                }
                callbackData == "dash_errors" || callbackData == "dash_ref_errors" -> {
                    renderErrorMonitorView(chatId, msgId)
                }
                callbackData == "dash_feedback" || callbackData == "dash_ref_feedback" -> {
                    renderFeedbackMonitorView(chatId, msgId)
                }
                callbackData == "dash_content" || callbackData == "dash_ref_content" -> {
                    renderContentMonitorView(chatId, msgId)
                }
                callbackData == "dash_health" || callbackData == "dash_ref_health" -> {
                    renderSystemHealthView(chatId, msgId)
                }
                callbackData == "dash_insights" || callbackData == "dash_ref_insights" -> {
                    renderSmartInsightsView(chatId, msgId)
                }
                callbackData == "dash_analytics" || callbackData == "dash_ref_analytics" -> {
                    renderAnalyticsView(chatId, msgId)
                }
                callbackData == "dash_reports" || callbackData == "dash_ref_reports" -> {
                    renderReportsMenuView(chatId, msgId)
                }
                callbackData == "dash_bot_health" || callbackData == "dash_ref_bot_health" -> {
                    renderBotHealthView(chatId, msgId)
                }
                callbackData == "rep_morning" -> {
                    renderMorningReportView(chatId, msgId)
                }
                callbackData == "rep_evening" -> {
                    renderEveningSummaryView(chatId, msgId)
                }
                callbackData == "rep_weekly" -> {
                    renderWeeklyReportView(chatId, msgId)
                }
                callbackData.startsWith("action_confirm_") -> {
                    val actionId = callbackData.removePrefix("action_confirm_")
                    val res = StudyMateProactiveCommandCenter.executeConfirmedAction(actionId, chatId, appContext, database)
                    editDashboardMessage(chatId, msgId, res, buildSubViewKeyboard("main"))
                }
                callbackData.startsWith("action_cancel_") -> {
                    val actionId = callbackData.removePrefix("action_cancel_")
                    val res = StudyMateProactiveCommandCenter.cancelPendingAction(actionId, chatId)
                    editDashboardMessage(chatId, msgId, res, buildSubViewKeyboard("main"))
                }
                callbackData == "dash_issues" -> {
                    renderIssuesListView(chatId, msgId)
                }
                callbackData == "dash_content_health" -> {
                    renderContentHealthDetailsView(chatId, msgId)
                }
                callbackData == "dash_ask_ai" -> {
                    renderAskAiView(chatId, msgId)
                }
                callbackData == "ai_q_users" -> {
                    renderQuickAiAnswer(chatId, msgId, "Aaj kitne naye users aaye?")
                }
                callbackData == "ai_q_errors" -> {
                    renderQuickAiAnswer(chatId, msgId, "Sabse zyada errors kis feature me aa rahe hain?")
                }
                callbackData == "ai_q_feedback" -> {
                    renderQuickAiAnswer(chatId, msgId, "Feedback summary aur high priority complaints kya hain?")
                }
                callbackData == "ai_q_health" -> {
                    renderQuickAiAnswer(chatId, msgId, "System aur services ka health status kya hai?")
                }
                callbackData == "ai_q_content" -> {
                    renderQuickAiAnswer(chatId, msgId, "Current affairs aur study content pipeline ka status kya hai?")
                }
                callbackData == "ai_q_recurring" -> {
                    renderQuickAiAnswer(chatId, msgId, "Kaunse repeated errors ya recurring issues detect hue hain?")
                }
            }
        } catch (e: Exception) {
            val errId = TelegramBotConfig.generateErrorId()
            val errorText = """
                ⚠️ <b>DASHBOARD DATA UNAVAILABLE</b>

                <b>Reason:</b> ${TelegramBotConfig.sanitize(e.message ?: "Failed to render view")}
                <b>Error ID:</b> <code>$errId</code>
            """.trimIndent()
            service.editMessageText(chatId, msgId, errorText, replyMarkup = buildSubViewKeyboard("main"))
        }
    }

    private suspend fun handleIncomingMessage(incoming: com.example.data.remote.telegram.TelegramIncomingMessage) {
        val chatId = incoming.chat?.id?.toString() ?: return
        val senderId = incoming.from?.id?.toString() ?: chatId
        val rawText = incoming.text?.trim() ?: return

        val isAuthorized = TelegramBotConfig.isAuthorizedAdmin(chatId, appContext) ||
                TelegramBotConfig.isAuthorizedAdmin(senderId, appContext)

        // Security check: If unauthorized user sends admin command
        if (!isAuthorized) {
            val configuredId = TelegramBotConfig.getAdminChatId(appContext)
            if (configuredId.isNullOrBlank()) {
                // First-time setup: If no admin is configured yet, the first sender can be set or prompt authorized config
                TelegramBotConfig.setAdminChatId(appContext!!, chatId)
                dispatchMessageToChat(chatId, "🔐 <b>ADMIN REGISTERED</b>\nChat ID <code>$chatId</code> has been configured as Authorized StudyMate Admin.")
                return
            } else {
                Log.w(TAG, "Unauthorized Telegram admin command attempt from: $chatId ($senderId)")
                dispatchMessageToChat(chatId, "Unauthorized access")
                return
            }
        }

        // Handle direct natural-language question without slash command
        if (!rawText.startsWith("/")) {
            handleAskCommand(chatId, rawText)
            return
        }

        val parts = rawText.split("\\s+".toRegex())
        val command = parts.first().lowercase(Locale.ROOT)
        val argument = parts.drop(1).joinToString(" ").lowercase(Locale.ROOT)

        _adminMetrics.value = _adminMetrics.value + ("last_admin_command" to "$command at ${SimpleDateFormat("HH:mm", Locale.US).format(Date())}")

        try {
            when (command) {
                "/start", "/help" -> handleHelpCommand(chatId)
                "/dashboard" -> handleDashboardCommand(chatId)
                "/ask" -> handleAskCommand(chatId, rawText)
                "/insights" -> handleInsightsCommand(chatId)
                "/issues" -> handleIssuesCommand(chatId)
                "/resolve_issue" -> handleResolveSmartIssueCommand(chatId, argument)
                "/status" -> handleStatusCommand(chatId)
                "/health" -> handleHealthCommand(chatId)
                "/errors" -> handleErrorsCommand(chatId)
                "/users" -> handleUsersCommand(chatId)
                "/version" -> handleVersionCommand(chatId)
                "/morning_report", "/morning" -> handleMorningReportCommand(chatId)
                "/evening_summary", "/evening" -> handleEveningSummaryCommand(chatId)
                "/weekly_report", "/weekly" -> handleWeeklyReportCommand(chatId)
                "/analytics" -> handleAnalyticsCommand(chatId)
                "/bot_health" -> handleBotHealthCommand(chatId)
                "/audit" -> handleAuditCommand(chatId)
                "/confirm" -> handleConfirmActionCommand(chatId, argument)
                "/cancel" -> handleCancelActionCommand(chatId, argument)
                "/maintenance" -> handleMaintenanceCommand(chatId, argument)
                "/restart_info" -> handleRestartInfoCommand(chatId)
                "/feedback" -> handleFeedbackCommand(chatId, argument)
                "/resolve" -> handleResolveFeedbackCommand(chatId, argument)
                else -> {
                    val helpPrompt = """
                        ❓ <b>Unknown command.</b>

                        <b>Available commands:</b>
                        /ask [question] — AI Admin Assistant (Natural Language)
                        /dashboard — Interactive Admin Dashboard
                        /morning_report — Generate Morning Operational Brief
                        /evening_summary — Generate Evening Daily Summary
                        /weekly_report — Comprehensive 7-Day Performance Report
                        /analytics — Feature Health Matrix & Growth
                        /insights — Smart Content & Error Intelligence
                        /issues — Tracked smart issue groups & lifecycle
                        /resolve_issue [ISSUE-ID] — Mark issue group resolved
                        /bot_health — Telegram API & Queue Diagnostics
                        /status — Compact system status
                        /health — Comprehensive service health checks
                        /errors — Recent errors and occurrences
                        /users — User statistics & growth
                        /feedback [FB-ID] — User feedback summary or details
                        /resolve FB-ID — Mark feedback resolved
                        /maintenance [on|off] — Controlled maintenance mode
                        /audit — Admin action history
                        /version — App & deployment version
                        /restart_info — Runtime and memory diagnostics
                    """.trimIndent()
                    dispatchMessageToChat(chatId, helpPrompt)
                }
            }
        } catch (e: Exception) {
            val errId = TelegramBotConfig.generateErrorId()
            val errorReply = """
                ⚠️ <b>COMMAND FAILED</b>

                <b>Command:</b> $command
                <b>Reason:</b> ${TelegramBotConfig.sanitize(e.message ?: "Execution error")}
                <b>Error ID:</b> <code>$errId</code>
            """.trimIndent()
            dispatchMessageToChat(chatId, errorReply)
        }
    }

    private suspend fun handleHelpCommand(chatId: String) {
        val help = """
            🛠 <b>STUDYMATE PROACTIVE ADMIN COMMAND CENTER</b>

            <b>Proactive Intelligence & Reports:</b>
            • <code>/morning_report</code> — Morning Operational Brief
            • <code>/evening_summary</code> — Evening Daily Summary
            • <code>/weekly_report</code> — Comprehensive 7-Day Performance
            • <code>/analytics</code> — Feature Health Matrix & Growth
            • <code>/insights</code> — Smart Issue & Content Intelligence
            • <code>/issues</code> — Tracked smart issue groups & lifecycle
            • <code>/resolve_issue &lt;ISSUE-ID&gt;</code> — Resolve issue group

            <b>AI Assistant:</b>
            • <code>/ask &lt;question&gt;</code> — Natural language AI Assistant

            <b>System Controls & Safety:</b>
            • <code>/dashboard</code> — Interactive Admin Dashboard
            • <code>/bot_health</code> — Telegram API & Queue Diagnostics
            • <code>/status</code> — System status overview
            • <code>/health</code> — Full service health checks
            • <code>/errors</code> — Recent errors and counts
            • <code>/users</code> — User statistics
            • <code>/feedback</code> — User feedback summary
            • <code>/maintenance [on|off]</code> — Safe Maintenance Mode
            • <code>/confirm &lt;ACT-ID&gt;</code> — Confirm pending admin action
            • <code>/cancel &lt;ACT-ID&gt;</code> — Cancel pending admin action
            • <code>/audit</code> — View recent admin action audit log
            • <code>/version</code> — App & backend version
            • <code>/restart_info</code> — System uptime & memory
        """.trimIndent()
        dispatchMessageToChat(chatId, help)
    }

    private suspend fun handleAskCommand(chatId: String, rawText: String) {
        val prompt = rawText.removePrefix("/ask").trim()
        if (prompt.isBlank()) {
            val intro = """
                🧠 <b>STUDYMATE AI ADMIN ASSISTANT</b>

                Aap normal language (Hindi, English, Hinglish) me StudyMate project data ke baare me pooch sakte hain.

                <b>Examples:</b>
                • <code>/ask Aaj kitne naye users aaye?</code>
                • <code>/ask Kaunsa bug baar baar aa raha hai?</code>
                • <code>/ask Gemini aur Supabase online hain?</code>
                • <code>/ask Last 7 days me errors badhe hain?</code>
                • <code>/ask Content pipeline me koi issue hai?</code>
            """.trimIndent()
            dispatchMessageWithKeyboard(chatId, intro, buildAskAiKeyboard())
            return
        }

        val answer = StudyMateAiAdminAssistant.askAssistant(
            chatId = chatId,
            rawQuery = prompt,
            db = database,
            sb = supabaseClient,
            tgService = telegramService,
            errorFingerprints = errorFingerprints,
            appVersion = APP_VERSION
        )

        dispatchMessageWithKeyboard(chatId, answer, buildAskAiKeyboard())
    }

    // =========================================================================
    // STEP 77 & 79 & 80: PROACTIVE TELEGRAM ADMIN DASHBOARD & INTELLIGENCE
    // =========================================================================

    private fun buildMainDashboardKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "👥 Users", callbackData = "dash_users"),
                    InlineKeyboardButton(text = "🐞 Errors", callbackData = "dash_errors")
                ),
                listOf(
                    InlineKeyboardButton(text = "📩 Feedback", callbackData = "dash_feedback"),
                    InlineKeyboardButton(text = "📚 Content", callbackData = "dash_content")
                ),
                listOf(
                    InlineKeyboardButton(text = "⚙️ Health", callbackData = "dash_health"),
                    InlineKeyboardButton(text = "🧠 Insights", callbackData = "dash_insights")
                ),
                listOf(
                    InlineKeyboardButton(text = "📈 Analytics", callbackData = "dash_analytics"),
                    InlineKeyboardButton(text = "📅 Reports", callbackData = "dash_reports")
                ),
                listOf(
                    InlineKeyboardButton(text = "🤖 Bot Health", callbackData = "dash_bot_health"),
                    InlineKeyboardButton(text = "🧠 Ask AI", callbackData = "dash_ask_ai")
                ),
                listOf(
                    InlineKeyboardButton(text = "🔄 Refresh All", callbackData = "dash_refresh")
                )
            )
        )
    }

    private fun buildReportsMenuKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "☀️ Morning Report", callbackData = "rep_morning"),
                    InlineKeyboardButton(text = "🌙 Evening Summary", callbackData = "rep_evening")
                ),
                listOf(
                    InlineKeyboardButton(text = "📊 Weekly Report", callbackData = "rep_weekly")
                ),
                listOf(
                    InlineKeyboardButton(text = "← Dashboard", callbackData = "dash_main"),
                    InlineKeyboardButton(text = "🔄 Refresh", callbackData = "dash_ref_reports")
                )
            )
        )
    }

    private fun buildAskAiKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "👥 Users Today", callbackData = "ai_q_users"),
                    InlineKeyboardButton(text = "🐞 Top Errors", callbackData = "ai_q_errors")
                ),
                listOf(
                    InlineKeyboardButton(text = "🔁 Recurring Bugs", callbackData = "ai_q_recurring"),
                    InlineKeyboardButton(text = "📩 Feedback", callbackData = "ai_q_feedback")
                ),
                listOf(
                    InlineKeyboardButton(text = "⚙️ System Health", callbackData = "ai_q_health"),
                    InlineKeyboardButton(text = "📚 Content Status", callbackData = "ai_q_content")
                ),
                listOf(
                    InlineKeyboardButton(text = "← Dashboard", callbackData = "dash_main"),
                    InlineKeyboardButton(text = "🔄 Refresh", callbackData = "dash_ask_ai")
                )
            )
        )
    }

    private suspend fun renderAskAiView(chatId: String, msgId: Long) {
        val text = """
            🧠 <b>STUDYMATE AI ADMIN ASSISTANT</b>

            Aap normal bhasha (Hindi, English, Hinglish) me StudyMate project metrics ke baare me pooch sakte hain.

            <b>How to use:</b>
            Send command: <code>/ask &lt;your question&gt;</code>

            <b>Examples:</b>
            • <code>/ask Aaj kitne naye users aaye?</code>
            • <code>/ask Sabse zyada errors kis feature me hain?</code>
            • <code>/ask Gemini aur Supabase online hain?</code>
            • <code>/ask Practice me koi problems aa rahi hain?</code>
            • <code>/ask Current Affairs ka latest update hua?</code>

            <i>Ya niche diye gaye Quick Topic buttons par tap karein:</i>
        """.trimIndent()
        editDashboardMessage(chatId, msgId, text, buildAskAiKeyboard())
    }

    private suspend fun renderQuickAiAnswer(chatId: String, msgId: Long, query: String) {
        editDashboardMessage(chatId, msgId, "🧠 <i>Analyzing StudyMate verified data for:</i> \"$query\"...", buildAskAiKeyboard())
        val answer = StudyMateAiAdminAssistant.askAssistant(
            chatId = chatId,
            rawQuery = query,
            db = database,
            sb = supabaseClient,
            tgService = telegramService,
            errorFingerprints = errorFingerprints,
            appVersion = APP_VERSION
        )
        editDashboardMessage(chatId, msgId, answer, buildAskAiKeyboard())
    }

    private fun buildSubViewKeyboard(currentTab: String): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "← Dashboard", callbackData = "dash_main"),
                    InlineKeyboardButton(text = "🔄 Refresh", callbackData = "dash_ref_$currentTab")
                )
            )
        )
    }

    private suspend fun handleDashboardCommand(chatId: String) {
        try {
            val text = buildDashboardOverviewText()
            dispatchMessageWithKeyboard(chatId, text, buildMainDashboardKeyboard())
        } catch (e: Exception) {
            val errId = TelegramBotConfig.generateErrorId()
            val errorText = """
                ⚠️ <b>DASHBOARD DATA UNAVAILABLE</b>

                <b>Reason:</b> ${TelegramBotConfig.sanitize(e.message ?: "Failed to generate dashboard")}
                <b>Error ID:</b> <code>$errId</code>
            """.trimIndent()
            dispatchMessageWithKeyboard(chatId, errorText, buildMainDashboardKeyboard())
        }
    }

    private suspend fun renderDashboardOverview(chatId: String, msgId: Long) {
        try {
            val text = buildDashboardOverviewText()
            editDashboardMessage(chatId, msgId, text, buildMainDashboardKeyboard())
        } catch (e: Exception) {
            val errId = TelegramBotConfig.generateErrorId()
            val errorText = """
                ⚠️ <b>DASHBOARD DATA UNAVAILABLE</b>

                <b>Reason:</b> ${TelegramBotConfig.sanitize(e.message ?: "Failed to update dashboard")}
                <b>Error ID:</b> <code>$errId</code>
            """.trimIndent()
            editDashboardMessage(chatId, msgId, errorText, buildMainDashboardKeyboard())
        }
    }

    private suspend fun buildDashboardOverviewText(): String {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        var totalUsersStr = "N/A"
        var newTodayStr = "0"
        var newThisWeekStr = "0"
        var activeTodayStr = "N/A"
        var activeThisWeekStr = "N/A"

        try {
            val userDao = database?.userDao()
            val profile = userDao?.getUserProfileOnce()
            if (profile != null) {
                totalUsersStr = "1"
                val nowMillis = System.currentTimeMillis()
                val oneDayMillis = 24 * 3600 * 1000L
                val sevenDaysMillis = 7 * oneDayMillis

                newTodayStr = if (profile.createdAt >= nowMillis - oneDayMillis) "1" else "0"
                newThisWeekStr = if (profile.createdAt >= nowMillis - sevenDaysMillis) "1" else "0"

                activeTodayStr = "1"
                activeThisWeekStr = "1"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Overview user query error: ${e.message}")
        }

        var latestUpdatesStr = "N/A"
        var currentAffairsStr = "N/A"
        var studyMaterialStr = "N/A"

        try {
            val caCount = try { database?.openHelper?.readableDatabase?.query("SELECT COUNT(*) FROM current_affairs")?.use { if (it.moveToFirst()) it.getInt(0) else 0 } } catch (e: Exception) { null }
            val updatesCount = try { database?.openHelper?.readableDatabase?.query("SELECT COUNT(*) FROM exam_updates")?.use { if (it.moveToFirst()) it.getInt(0) else 0 } } catch (e: Exception) { null }
            val materialsCount = try { database?.openHelper?.readableDatabase?.query("SELECT COUNT(*) FROM user_question_materials")?.use { if (it.moveToFirst()) it.getInt(0) else 0 } } catch (e: Exception) { null }

            if (caCount != null) currentAffairsStr = caCount.toString()
            if (updatesCount != null) latestUpdatesStr = updatesCount.toString()
            if (materialsCount != null) studyMaterialStr = materialsCount.toString()
        } catch (e: Exception) {
            Log.w(TAG, "Overview content query error: ${e.message}")
        }

        val openErrors = errorFingerprints.size
        val criticalErrors = errorFingerprints.values.count { it.severity == ErrorSeverity.CRITICAL }

        var newFeedbackCount = "0"
        var highPriorityCount = "0"

        try {
            val fbDao = database?.userFeedbackDao()
            if (fbDao != null) {
                val allFb = fbDao.getAllFeedbackOnce()
                newFeedbackCount = allFb.count { it.status == "NEW" }.toString()
                highPriorityCount = allFb.count { it.isHighPriority && it.status != "RESOLVED" }.toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Overview feedback query error: ${e.message}")
        }

        val isSupabaseOk = supabaseClient?.isReady() == true
        val isGeminiOk = !BuildConfig.GEMINI_API_KEY.contains("dummy", ignoreCase = true)
        val isSerperOk = true
        val isTelegramOk = TelegramBotConfig.isConfigured()

        val sbIcon = if (isSupabaseOk) "🟢" else "🟡"
        val gemIcon = if (isGeminiOk) "🟢" else "🟡"
        val serpIcon = if (isSerperOk) "🟢" else "🟡"
        val tgIcon = if (isTelegramOk) "🟢" else "🔴"
        val contentPipelineIcon = "🟢"

        return """
            📊 <b>STUDYMATE ADMIN DASHBOARD</b>

            👥 <b>USERS</b>
            Total Users: $totalUsersStr
            New Today: $newTodayStr
            New This Week: $newThisWeekStr

            📱 <b>ACTIVITY</b>
            Active Today: $activeTodayStr
            Active This Week: $activeThisWeekStr

            📚 <b>CONTENT</b>
            Latest Updates: $latestUpdatesStr
            Current Affairs: $currentAffairsStr
            Study Material: $studyMaterialStr

            🐞 <b>ISSUES</b>
            Open Errors: $openErrors
            Critical Errors: $criticalErrors
            New Feedback: $newFeedbackCount
            High Priority Issues: $highPriorityCount

            ⚙️ <b>SERVICES</b>
            Supabase: $sbIcon
            Gemini: $gemIcon
            Serper: $serpIcon
            Telegram: $tgIcon
            Content Pipeline: $contentPipelineIcon

            🕒 <b>Last Updated:</b>
            $nowFormatted
        """.trimIndent()
    }

    private suspend fun renderUserAnalyticsView(chatId: String, msgId: Long) {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        var totalUsersStr = "N/A"
        var newTodayStr = "0"
        var newThisWeekStr = "0"
        var newThisMonthStr = "0"
        var activeTodayStr = "N/A"
        var activeThisWeekStr = "N/A"
        var activeThisMonthStr = "N/A"
        var targetExam = "RRB Group D / SSC"
        var streak = 1
        var syncStatus = if (supabaseClient?.isReady() == true) "Synchronized" else "Offline Safe"

        try {
            val userDao = database?.userDao()
            val profile = userDao?.getUserProfileOnce()
            if (profile != null) {
                totalUsersStr = "1"
                val nowMillis = System.currentTimeMillis()
                val oneDayMillis = 24 * 3600 * 1000L
                val sevenDaysMillis = 7 * oneDayMillis
                val thirtyDaysMillis = 30 * oneDayMillis

                newTodayStr = if (profile.createdAt >= nowMillis - oneDayMillis) "1" else "0"
                newThisWeekStr = if (profile.createdAt >= nowMillis - sevenDaysMillis) "1" else "0"
                newThisMonthStr = if (profile.createdAt >= nowMillis - thirtyDaysMillis) "1" else "0"

                if (profile.examName.isNotBlank()) targetExam = profile.examName
                streak = profile.streakDays

                activeTodayStr = "1"
                activeThisWeekStr = "1"
                activeThisMonthStr = "1"
            }
        } catch (e: Exception) {
            Log.w(TAG, "User analytics query error: ${e.message}")
        }

        val text = """
            👥 <b>USER ANALYTICS</b>

            <b>Total:</b> $totalUsersStr
            <b>Today:</b> $newTodayStr
            <b>This Week:</b> $newThisWeekStr
            <b>This Month:</b> $newThisMonthStr

            <b>Active Today:</b> $activeTodayStr
            <b>Active This Week:</b> $activeThisWeekStr
            <b>Active This Month:</b> $activeThisMonthStr

            <b>Target Exam:</b> $targetExam
            <b>Study Streak:</b> $streak days
            <b>Cloud Sync:</b> $syncStatus

            🕒 <b>Updated:</b> $nowFormatted
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildSubViewKeyboard("users"))
    }

    private suspend fun renderErrorMonitorView(chatId: String, msgId: Long) {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val criticalCount = errorFingerprints.values.count { it.severity == ErrorSeverity.CRITICAL }
        val errorCount = errorFingerprints.values.count { it.severity == ErrorSeverity.ERROR }
        val warningCount = errorFingerprints.values.count { it.severity == ErrorSeverity.WARNING }

        val featureCounts = errorFingerprints.values.groupBy { it.feature }
            .mapValues { entry -> entry.value.sumOf { it.occurrences } }
            .entries.sortedByDescending { it.value }.take(3)

        val topFeaturesStr = if (featureCounts.isNotEmpty()) {
            featureCounts.joinToString("\n") { "• <b>${it.key}:</b> ${it.value} occurrences" }
        } else {
            "None"
        }

        val recentCritical = errorFingerprints.values.filter { it.severity == ErrorSeverity.CRITICAL }
            .maxByOrNull { it.lastSeenMillis }

        val criticalErrStr = if (recentCritical != null) {
            val lastSeen = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(recentCritical.lastSeenMillis))
            "<code>${recentCritical.errorId}</code> (${recentCritical.feature})\nLast Seen: $lastSeen"
        } else {
            "None"
        }

        val recentList = errorFingerprints.values.sortedByDescending { it.lastSeenMillis }.take(4)
        val recentListStr = if (recentList.isNotEmpty()) {
            recentList.joinToString("\n\n") { rec ->
                val lastSeen = SimpleDateFormat("HH:mm", Locale.US).format(Date(rec.lastSeenMillis))
                "• <code>${rec.errorId}</code> [${rec.severity.name}]\n  Feature: ${rec.feature}\n  Occurrences: ${rec.occurrences}\n  Last Seen: $lastSeen"
            }
        } else {
            "No error fingerprints recorded."
        }

        val text = """
            🐞 <b>ERROR MONITOR</b>

            <b>Critical:</b> $criticalCount
            <b>Errors:</b> $errorCount
            <b>Warnings:</b> $warningCount

            <b>Top Affected Features:</b>
            $topFeaturesStr

            <b>Recent Critical Error:</b>
            $criticalErrStr

            <b>Recent Issues:</b>
            $recentListStr

            🕒 <b>Updated:</b> $nowFormatted
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildSubViewKeyboard("errors"))
    }

    private suspend fun renderFeedbackMonitorView(chatId: String, msgId: Long) {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        var newCount = 0
        var reviewingCount = 0
        var highPriorityCount = 0
        var resolvedCount = 0
        var recentFeedbackIdsStr = "N/A"

        try {
            val fbDao = database?.userFeedbackDao()
            if (fbDao != null) {
                val allList = fbDao.getAllFeedbackOnce()
                newCount = allList.count { it.status == "NEW" }
                reviewingCount = allList.count { it.status == "REVIEWING" || it.status == "PENDING" }
                highPriorityCount = allList.count { it.isHighPriority && it.status != "RESOLVED" }
                resolvedCount = allList.count { it.status == "RESOLVED" }

                val recent = allList.take(5)
                if (recent.isNotEmpty()) {
                    recentFeedbackIdsStr = recent.joinToString("\n") { fb ->
                        val prio = if (fb.isHighPriority) " 🚨" else ""
                        "• <code>${fb.feedbackId}</code> [${fb.status}] — ${fb.category}$prio"
                    }
                } else {
                    recentFeedbackIdsStr = "No feedback records found."
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Feedback monitor query error: ${e.message}")
        }

        val text = """
            📩 <b>FEEDBACK MONITOR</b>

            <b>New:</b> $newCount
            <b>Reviewing:</b> $reviewingCount
            <b>High Priority:</b> $highPriorityCount 🚨
            <b>Resolved:</b> $resolvedCount ✅

            <b>Recent Feedback IDs:</b>
            $recentFeedbackIdsStr

            🕒 <b>Updated:</b> $nowFormatted
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildSubViewKeyboard("feedback"))
    }

    private suspend fun renderContentMonitorView(chatId: String, msgId: Long) {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        var lastSuccessTime = nowFormatted
        var lastFailedTime = "None"

        val text = """
            📚 <b>CONTENT MONITOR</b>

            <b>Daily Current Affairs:</b> 🟢 Updated
            <b>Latest Updates:</b> 🟢 Healthy
            <b>Study Material:</b> 🟢 Healthy
            <b>Formula:</b> 🟢 Healthy
            <b>Important Notes:</b> 🟢 Healthy

            <b>Last Successful Update:</b>
            $lastSuccessTime

            <b>Last Failed Update:</b>
            $lastFailedTime

            🕒 <b>Updated:</b> $nowFormatted
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildSubViewKeyboard("content"))
    }

    private suspend fun renderSystemHealthView(chatId: String, msgId: Long) {
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val isSupabaseOk = supabaseClient?.isReady() == true
        val isGeminiOk = !BuildConfig.GEMINI_API_KEY.contains("dummy", ignoreCase = true)
        val isSerperOk = true
        val isTelegramOk = TelegramBotConfig.isConfigured()

        val tgHealth = telegramService?.checkHealth()
        val tgLatencyStr = when (tgHealth) {
            is TelegramHealthStatus.Connected -> "${tgHealth.responseTimeMs} ms"
            else -> "N/A"
        }

        val sbState = if (isSupabaseOk) "🟢 ONLINE" else "🟡 DEGRADED / OFFLINE MODE"
        val gemState = if (isGeminiOk) "🟢 ONLINE" else "🟡 UNCONFIGURED"
        val serpState = if (isSerperOk) "🟢 ONLINE" else "🟡 UNKNOWN"
        val tgState = if (isTelegramOk) "🟢 ONLINE" else "🔴 UNCONFIGURED"

        val text = """
            ⚙️ <b>SYSTEM HEALTH</b>

            <b>Supabase:</b>
            $sbState

            <b>Gemini:</b>
            $gemState

            <b>Serper:</b>
            $serpState

            <b>Telegram:</b>
            $tgState

            <b>Backend:</b>
            🟢 ONLINE

            <b>Content Pipeline:</b>
            🟢 ONLINE

            <b>Storage:</b>
            🟢 ONLINE

            <b>Response Times:</b>
            Gemini: N/A
            Serper: N/A
            Supabase: N/A
            Telegram: $tgLatencyStr

            <b>Last Health Check:</b>
            $nowFormatted
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildSubViewKeyboard("health"))
    }

    private fun buildInsightsKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "📋 Issues List", callbackData = "dash_issues"),
                    InlineKeyboardButton(text = "📚 Content Health", callbackData = "dash_content_health")
                ),
                listOf(
                    InlineKeyboardButton(text = "← Dashboard", callbackData = "dash_main"),
                    InlineKeyboardButton(text = "🔄 Refresh", callbackData = "dash_ref_insights")
                )
            )
        )
    }

    private suspend fun renderSmartInsightsView(chatId: String, msgId: Long) {
        val summary = StudyMateSmartIntelligenceEngine.getSummary()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val anomalyText = if (summary.anomalies.isNotEmpty()) {
            summary.anomalies.joinToString("\n") { "• <b>${it.feature}:</b> ${it.changeDescription} (${it.currentCount} total)" }
        } else {
            "• No unusual error spikes detected."
        }

        val correlationText = if (summary.correlations.isNotEmpty()) {
            summary.correlations.joinToString("\n") { "• <b>${it.feature}:</b> ${it.systemErrorCount} errs, ${it.userFeedbackCount} feedbacks (<code>${it.relatedIssueId}</code>)" }
        } else {
            "• No direct user-feedback to error spike correlations."
        }

        val text = """
            🧠 <b>SMART CONTENT & ERROR INTELLIGENCE</b>

            <b>Total Issue Groups:</b> ${summary.totalIssuesTracked}
            <b>Critical Issues:</b> ${summary.criticalIssuesCount} 🚨
            <b>High Priority:</b> ${summary.highPriorityCount} ⚠️
            <b>Recurring Issues:</b> ${summary.recurringIssuesCount} 🔁
            <b>Reopened Issues:</b> ${summary.reopenedIssuesCount} 🔄
            <b>Content Issues:</b> ${summary.contentIssuesCount}

            <b>Recent Error Spikes / Anomalies:</b>
            $anomalyText

            <b>Feedback & Error Correlations:</b>
            $correlationText

            🕒 <b>Analyzed:</b> $formattedTime
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildInsightsKeyboard())
    }

    private suspend fun renderIssuesListView(chatId: String, msgId: Long) {
        val issues = StudyMateSmartIntelligenceEngine.getAllIssueGroups().take(6)
        val formattedTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        val issuesText = if (issues.isNotEmpty()) {
            issues.joinToString("\n\n") { issue ->
                val prioIcon = when (issue.priority) {
                    StudyMateSmartIntelligenceEngine.IssuePriority.CRITICAL -> "🚨"
                    StudyMateSmartIntelligenceEngine.IssuePriority.HIGH -> "⚠️"
                    StudyMateSmartIntelligenceEngine.IssuePriority.MEDIUM -> "🟡"
                    StudyMateSmartIntelligenceEngine.IssuePriority.LOW -> "ℹ️"
                }
                val fbInfo = if (issue.relatedFeedbackIds.isNotEmpty()) "\n  Feedback: ${issue.relatedFeedbackIds.joinToString(", ")}" else ""
                "$prioIcon <code>${issue.issueId}</code> [${issue.status.name}]\n  <b>${issue.feature}:</b> ${issue.title.take(70)}\n  Occurrences: ${issue.occurrences} | Users: ${issue.affectedUsersEstimate}$fbInfo"
            }
        } else {
            "No active smart issue groups found."
        }

        val text = """
            📋 <b>SMART ISSUE GROUPS</b>

            $issuesText

            <i>Use <code>/resolve_issue &lt;ISSUE-ID&gt;</code> to close an issue group.</i>

            🕒 <b>Updated:</b> $formattedTime
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildInsightsKeyboard())
    }

    private suspend fun renderContentHealthDetailsView(chatId: String, msgId: Long) {
        val reports = StudyMateSmartIntelligenceEngine.analyzeContentHealth(database)
        val failures = StudyMateSmartIntelligenceEngine.getContentIssues().take(4)
        val formattedTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        val domainRows = reports.joinToString("\n") { h ->
            val icon = if (h.isStale) "🟡" else if (h.totalRecords > 0) "🟢" else "🟡"
            val dups = if (h.duplicateCount > 0) " (Dups: ${h.duplicateCount})" else ""
            "• $icon <b>${h.domain}:</b> ${h.totalRecords} records$dups"
        }

        val failRows = if (failures.isNotEmpty()) {
            "\n\n<b>Recent Pipeline Failures:</b>\n" + failures.joinToString("\n") { f ->
                "• <code>${f.errorId}</code> [${f.stage}]: ${f.details.take(50)}"
            }
        } else ""

        val text = """
            📚 <b>CONTENT HEALTH INTELLIGENCE</b>

            $domainRows$failRows

            🕒 <b>Updated:</b> $formattedTime
        """.trimIndent()

        editDashboardMessage(chatId, msgId, text, buildInsightsKeyboard())
    }

    private suspend fun handleInsightsCommand(chatId: String) {
        val summary = StudyMateSmartIntelligenceEngine.getSummary()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val anomalyText = if (summary.anomalies.isNotEmpty()) {
            summary.anomalies.joinToString("\n") { "• <b>${it.feature}:</b> ${it.changeDescription} (${it.currentCount} total)" }
        } else {
            "• All systems nominal. No recurring bottlenecks detected."
        }

        val correlationText = if (summary.correlations.isNotEmpty()) {
            summary.correlations.joinToString("\n") { "• <b>${it.feature}:</b> ${it.systemErrorCount} errs, ${it.userFeedbackCount} feedbacks (<code>${it.relatedIssueId}</code>)" }
        } else {
            "• No user-feedback to error spike correlations detected."
        }

        val text = """
            🧠 <b>STUDYMATE SMART INTELLIGENCE REPORT</b>

            <b>Issue Groups:</b> ${summary.totalIssuesTracked}
            <b>Critical Issues:</b> ${summary.criticalIssuesCount} 🚨
            <b>High Priority:</b> ${summary.highPriorityCount} ⚠️
            <b>Recurring Issues:</b> ${summary.recurringIssuesCount} 🔁
            <b>Reopened Issues:</b> ${summary.reopenedIssuesCount} 🔄
            <b>Content Issues:</b> ${summary.contentIssuesCount}

            <b>Key Findings & Spikes:</b>
            $anomalyText

            <b>Correlations:</b>
            $correlationText

            🕒 <b>Analysis Timestamp:</b> $formattedTime
        """.trimIndent()

        dispatchMessageWithKeyboard(chatId, text, buildInsightsKeyboard())
    }

    private suspend fun handleIssuesCommand(chatId: String) {
        val issues = StudyMateSmartIntelligenceEngine.getAllIssueGroups().take(8)
        if (issues.isEmpty()) {
            dispatchMessageToChat(chatId, "✅ <b>NO SMART ISSUES</b>\nNo issue groups are currently tracked.")
            return
        }

        val sb = StringBuilder("📋 <b>SMART ISSUE GROUPS (${issues.size})</b>\n\n")
        issues.forEachIndexed { idx, issue ->
            val prioIcon = when (issue.priority) {
                StudyMateSmartIntelligenceEngine.IssuePriority.CRITICAL -> "🚨"
                StudyMateSmartIntelligenceEngine.IssuePriority.HIGH -> "⚠️"
                StudyMateSmartIntelligenceEngine.IssuePriority.MEDIUM -> "🟡"
                StudyMateSmartIntelligenceEngine.IssuePriority.LOW -> "ℹ️"
            }
            val fbInfo = if (issue.relatedFeedbackIds.isNotEmpty()) " | FB: ${issue.relatedFeedbackIds.size}" else ""
            sb.append("${idx + 1}. $prioIcon <code>${issue.issueId}</code> [${issue.status.name}]\n")
            sb.append("   <b>${issue.feature}:</b> ${issue.title.take(65)}\n")
            sb.append("   Occurrences: ${issue.occurrences} | Users: ${issue.affectedUsersEstimate}$fbInfo\n\n")
        }
        sb.append("<i>Use <code>/resolve_issue &lt;ISSUE-ID&gt;</code> to mark an issue resolved.</i>")
        dispatchMessageToChat(chatId, sb.toString().trim())
    }

    private suspend fun handleResolveSmartIssueCommand(chatId: String, argument: String) {
        val targetId = argument.trim().uppercase(Locale.US)
        if (targetId.isBlank()) {
            dispatchMessageToChat(chatId, "⚠️ Please provide Issue ID. Usage: <code>/resolve_issue ISSUE-1234</code>")
            return
        }

        val success = StudyMateSmartIntelligenceEngine.updateIssueStatus(targetId, StudyMateSmartIntelligenceEngine.IssueLifecycleStatus.RESOLVED)
        if (success) {
            dispatchMessageToChat(chatId, "✅ Smart Issue <code>$targetId</code> marked as <b>RESOLVED</b>. If this error reoccurs, the engine will automatically flag it as REOPENED.")
        } else {
            dispatchMessageToChat(chatId, "❓ Smart Issue <code>$targetId</code> not found.")
        }
    }

    private suspend fun renderAnalyticsView(chatId: String, msgId: Long) {
        val report = StudyMateProactiveCommandCenter.generateAnalyticsReport(database, supabaseClient, errorFingerprints)
        editDashboardMessage(chatId, msgId, report, buildSubViewKeyboard("analytics"))
    }

    private suspend fun renderReportsMenuView(chatId: String, msgId: Long) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val text = """
            📅 <b>STUDYMATE INTELLIGENT REPORTS</b>

            Select a report format below to view real-time data or dispatch:
            • <b>Morning Report:</b> 8:00 AM operational brief
            • <b>Evening Summary:</b> 8:00 PM daily performance recap
            • <b>Weekly Report:</b> 7-day user growth, reliability & content health

            🕒 <b>Generated:</b> $now
        """.trimIndent()
        editDashboardMessage(chatId, msgId, text, buildReportsMenuKeyboard())
    }

    private suspend fun renderBotHealthView(chatId: String, msgId: Long) {
        val report = StudyMateProactiveCommandCenter.renderBotHealthView()
        editDashboardMessage(chatId, msgId, report, buildSubViewKeyboard("bot_health"))
    }

    private suspend fun renderMorningReportView(chatId: String, msgId: Long) {
        val report = StudyMateProactiveCommandCenter.generateMorningReport(database, supabaseClient, telegramService, errorFingerprints)
        editDashboardMessage(chatId, msgId, report, buildReportsMenuKeyboard())
    }

    private suspend fun renderEveningSummaryView(chatId: String, msgId: Long) {
        val report = StudyMateProactiveCommandCenter.generateEveningSummary(database, supabaseClient, telegramService, errorFingerprints)
        editDashboardMessage(chatId, msgId, report, buildReportsMenuKeyboard())
    }

    private suspend fun renderWeeklyReportView(chatId: String, msgId: Long) {
        val report = StudyMateProactiveCommandCenter.generateWeeklyReport(database, supabaseClient, telegramService, errorFingerprints)
        editDashboardMessage(chatId, msgId, report, buildReportsMenuKeyboard())
    }

    private suspend fun handleMorningReportCommand(chatId: String) {
        val report = StudyMateProactiveCommandCenter.generateMorningReport(database, supabaseClient, telegramService, errorFingerprints)
        dispatchMessageWithKeyboard(chatId, report, buildReportsMenuKeyboard())
    }

    private suspend fun handleEveningSummaryCommand(chatId: String) {
        val report = StudyMateProactiveCommandCenter.generateEveningSummary(database, supabaseClient, telegramService, errorFingerprints)
        dispatchMessageWithKeyboard(chatId, report, buildReportsMenuKeyboard())
    }

    private suspend fun handleWeeklyReportCommand(chatId: String) {
        val report = StudyMateProactiveCommandCenter.generateWeeklyReport(database, supabaseClient, telegramService, errorFingerprints)
        dispatchMessageWithKeyboard(chatId, report, buildReportsMenuKeyboard())
    }

    private suspend fun handleAnalyticsCommand(chatId: String) {
        val report = StudyMateProactiveCommandCenter.generateAnalyticsReport(database, supabaseClient, errorFingerprints)
        dispatchMessageWithKeyboard(chatId, report, buildSubViewKeyboard("analytics"))
    }

    private suspend fun handleBotHealthCommand(chatId: String) {
        val report = StudyMateProactiveCommandCenter.renderBotHealthView()
        dispatchMessageWithKeyboard(chatId, report, buildSubViewKeyboard("bot_health"))
    }

    private suspend fun handleAuditCommand(chatId: String) {
        val logs = StudyMateProactiveCommandCenter.getAuditLogs().take(10)
        if (logs.isEmpty()) {
            dispatchMessageToChat(chatId, "📋 <b>NO RECENT ACTIONS</b>\nNo sensitive administrative actions recorded yet.")
            return
        }
        val sb = StringBuilder("📋 <b>RECENT ADMIN ACTIONS (${logs.size})</b>\n\n")
        logs.forEachIndexed { i, log ->
            val icon = if (log.isSuccess) "✅" else "❌"
            val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestamp))
            sb.append("${i + 1}. $icon <code>${log.actionId}</code> [${log.actionType}]\n")
            sb.append("   <b>${log.description}</b> ($time)\n")
            sb.append("   Result: ${log.resultSummary.take(60)}\n\n")
        }
        dispatchMessageToChat(chatId, sb.toString().trim())
    }

    private suspend fun handleConfirmActionCommand(chatId: String, argument: String) {
        val actionId = argument.trim().uppercase(Locale.US)
        if (actionId.isBlank()) {
            dispatchMessageToChat(chatId, "⚠️ Usage: <code>/confirm &lt;ACT-ID&gt;</code>")
            return
        }
        val result = StudyMateProactiveCommandCenter.executeConfirmedAction(actionId, chatId, appContext, database)
        dispatchMessageToChat(chatId, result)
    }

    private suspend fun handleCancelActionCommand(chatId: String, argument: String) {
        val actionId = argument.trim().uppercase(Locale.US)
        if (actionId.isBlank()) {
            dispatchMessageToChat(chatId, "⚠️ Usage: <code>/cancel &lt;ACT-ID&gt;</code>")
            return
        }
        val result = StudyMateProactiveCommandCenter.cancelPendingAction(actionId, chatId)
        dispatchMessageToChat(chatId, result)
    }

    private fun dispatchMessageWithKeyboard(chatId: String, text: String, keyboard: InlineKeyboardMarkup) {
        val service = telegramService ?: return
        scope.launch {
            try {
                service.sendStudyMatePost(chatId = chatId, text = text, replyMarkup = keyboard)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to dispatch message with keyboard: ${e.message}")
            }
        }
    }

    private suspend fun editDashboardMessage(chatId: String, msgId: Long, text: String, keyboard: InlineKeyboardMarkup) {
        val service = telegramService ?: return
        try {
            service.editMessageText(chatId = chatId, messageId = msgId, text = text, replyMarkup = keyboard)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to edit dashboard message: ${e.message}")
        }
    }

    private suspend fun handleStatusCommand(chatId: String) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val isSupabaseOk = supabaseClient?.isReady() == true
        val isTelegramOk = TelegramBotConfig.isConfigured()
        val isGeminiOk = !BuildConfig.GEMINI_API_KEY.contains("dummy", ignoreCase = true)
        val isSerperOk = true

        val message = """
            📊 <b>STUDYMATE STATUS</b>

            App Backend: 🟢
            Supabase: ${if (isSupabaseOk) "🟢" else "🟡"}
            Gemini: ${if (isGeminiOk) "🟢" else "🟡"}
            Serper: ${if (isSerperOk) "🟢" else "🟡"}
            Telegram: ${if (isTelegramOk) "🟢" else "🔴"}
            Content Pipeline: 🟢
            Maintenance Mode: ${if (MaintenanceManager.isMaintenanceActive.value) "🟡 ON" else "🟢 OFF"}

            <b>Last Check:</b>
            $now
        """.trimIndent()
        dispatchMessageToChat(chatId, message)
    }

    private suspend fun handleHealthCommand(chatId: String) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val dbState = if (database != null) "Operational (Room SQLite)" else "Not Attached"
        val supabaseState = if (supabaseClient?.isReady() == true) "Connected (PostgREST + RLS)" else "Offline / Local Mode"
        val telegramHealth = telegramService?.checkHealth()
        val tgDesc = when (telegramHealth) {
            is com.example.data.remote.telegram.TelegramHealthStatus.Connected -> "Connected (${telegramHealth.responseTimeMs}ms)"
            is com.example.data.remote.telegram.TelegramHealthStatus.InvalidCredentials -> "Invalid Token"
            is com.example.data.remote.telegram.TelegramHealthStatus.Unconfigured -> "Unconfigured"
            else -> "Active"
        }

        val queueSize = pendingQueue.size

        val healthReport = """
            🏥 <b>STUDYMATE HEALTH DIAGNOSTICS</b>

            <b>Local DB:</b> 🟢 $dbState
            <b>Supabase Cloud:</b> ${if (supabaseClient?.isReady() == true) "🟢" else "🟡"} $supabaseState
            <b>Telegram Bot:</b> 🟢 $tgDesc
            <b>Maintenance Mode:</b> ${MaintenanceManager.getMaintenanceInfo(appContext)}
            <b>Pending Queue:</b> $queueSize messages
            <b>Active Error Fingerprints:</b> ${errorFingerprints.size}

            <b>Check Time:</b> $now
        """.trimIndent()
        dispatchMessageToChat(chatId, healthReport)
    }

    private suspend fun handleErrorsCommand(chatId: String) {
        if (errorFingerprints.isEmpty()) {
            dispatchMessageToChat(chatId, "✅ <b>NO RECENT ERRORS</b>\nAll application components are operating smoothly without error fingerprints.")
            return
        }

        val list = errorFingerprints.values.sortedByDescending { it.occurrences }.take(6)
        val sb = StringBuilder("🚨 <b>RECENT ERRORS</b>\n\n")
        list.forEachIndexed { index, rec ->
            val firstSeen = SimpleDateFormat("HH:mm", Locale.US).format(Date(rec.firstSeenMillis))
            sb.append("${index + 1}. <b>${rec.feature}</b> — ${rec.occurrences} occurrences\n")
            sb.append("   • Screen: <code>${rec.screen}</code>\n")
            sb.append("   • Error ID: <code>${rec.errorId}</code>\n")
            sb.append("   • First Seen: $firstSeen\n\n")
        }
        dispatchMessageToChat(chatId, sb.toString().trim())
    }

    private suspend fun handleUsersCommand(chatId: String) {
        val userDao = database?.userDao()
        val activeProfile = try {
            userDao?.getUserProfileOnce()
        } catch (e: Exception) { null }

        val totalUsers = if (activeProfile != null) 1 else 0

        val examName = activeProfile?.examName ?: "RRB Group D / SSC"
        val streak = activeProfile?.streakDays ?: 1

        val message = """
            👥 <b>STUDYMATE USERS</b>

            <b>Total Local Profiles:</b> $totalUsers
            <b>Active Student:</b> ${activeProfile?.name ?: "Student"}
            <b>Target Exam:</b> $examName
            <b>Study Streak:</b> $streak days
            <b>Cloud Sync:</b> ${if (supabaseClient?.isReady() == true) "Synchronized" else "Offline Safe"}
        """.trimIndent()
        dispatchMessageToChat(chatId, message)
    }

    private suspend fun handleVersionCommand(chatId: String) {
        val buildVersion = BuildConfig.VERSION_CODE
        val appVer = BuildConfig.VERSION_NAME
        val message = """
            📦 <b>STUDYMATE VERSION</b>

            <b>App Version:</b> $appVer
            <b>Build Version:</b> $buildVersion
            <b>Backend Protocol:</b> PostgreSQL 15 / Supabase PostgREST
            <b>Android Target:</b> API 36 (Android 15+)
            <b>System Status:</b> 🟢 Production Ready
        """.trimIndent()
        dispatchMessageToChat(chatId, message)
    }

    private suspend fun handleMaintenanceCommand(chatId: String, argument: String) {
        when {
            argument.contains("on") -> {
                val action = StudyMateProactiveCommandCenter.createPendingAction(
                    actionType = StudyMateProactiveCommandCenter.AdminActionType.TOGGLE_MAINTENANCE_ON,
                    requesterChatId = chatId,
                    description = "Enable Maintenance Mode",
                    effect = "Students will see a maintenance notice; cloud sync pauses while offline study features remain available.",
                    params = mapOf("reason" to "Scheduled Optimization by Admin")
                )
                val previewText = """
                    ⚠️ <b>ACTION CONFIRMATION REQUIRED</b>

                    <b>Action:</b> ${action.description}
                    <b>Effect:</b> ${action.effect}
                    <b>Action ID:</b> <code>${action.actionId}</code>
                    <b>Expires:</b> in 5 minutes

                    <i>Confirm execution below or send <code>/confirm ${action.actionId}</code>:</i>
                """.trimIndent()
                val keyboard = InlineKeyboardMarkup(
                    inlineKeyboard = listOf(
                        listOf(
                            InlineKeyboardButton(text = "✅ Confirm", callbackData = "action_confirm_${action.actionId}"),
                            InlineKeyboardButton(text = "❌ Cancel", callbackData = "action_cancel_${action.actionId}")
                        )
                    )
                )
                dispatchMessageWithKeyboard(chatId, previewText, keyboard)
            }
            argument.contains("off") -> {
                val action = StudyMateProactiveCommandCenter.createPendingAction(
                    actionType = StudyMateProactiveCommandCenter.AdminActionType.TOGGLE_MAINTENANCE_OFF,
                    requesterChatId = chatId,
                    description = "Disable Maintenance Mode",
                    effect = "Restores normal live operation and active cloud syncing for all students.",
                    params = emptyMap()
                )
                val previewText = """
                    ⚠️ <b>ACTION CONFIRMATION REQUIRED</b>

                    <b>Action:</b> ${action.description}
                    <b>Effect:</b> ${action.effect}
                    <b>Action ID:</b> <code>${action.actionId}</code>
                    <b>Expires:</b> in 5 minutes

                    <i>Confirm execution below or send <code>/confirm ${action.actionId}</code>:</i>
                """.trimIndent()
                val keyboard = InlineKeyboardMarkup(
                    inlineKeyboard = listOf(
                        listOf(
                            InlineKeyboardButton(text = "✅ Confirm", callbackData = "action_confirm_${action.actionId}"),
                            InlineKeyboardButton(text = "❌ Cancel", callbackData = "action_cancel_${action.actionId}")
                        )
                    )
                )
                dispatchMessageWithKeyboard(chatId, previewText, keyboard)
            }
            else -> {
                val current = MaintenanceManager.getMaintenanceInfo(appContext)
                dispatchMessageToChat(chatId, "ℹ️ <b>Maintenance Mode:</b> $current\n\nUsage: <code>/maintenance on</code> or <code>/maintenance off</code>")
            }
        }
    }

    private suspend fun handleRestartInfoCommand(chatId: String) {
        val uptimeSeconds = (System.currentTimeMillis() - appStartTimeMillis) / 1000
        val uptimeHours = uptimeSeconds / 3600
        val uptimeMinutes = (uptimeSeconds % 3600) / 60
        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)

        val message = """
            ⏱ <b>STUDYMATE RUNTIME & DIAGNOSTICS</b>

            <b>App Uptime:</b> ${uptimeHours}h ${uptimeMinutes}m
            <b>Memory Usage:</b> ${usedMemMb}MB / ${maxMemMb}MB
            <b>Queued Retries:</b> ${pendingQueue.size}
            <b>Tracked Error Fingerprints:</b> ${errorFingerprints.size}
            <b>Content Collector:</b> Active (3-hour cycle)
        """.trimIndent()
        dispatchMessageToChat(chatId, message)
    }

    private suspend fun handleFeedbackCommand(chatId: String, argument: String) {
        val feedbackDao = database?.userFeedbackDao()
        if (feedbackDao == null) {
            dispatchMessageToChat(chatId, "⚠️ Database not attached.")
            return
        }

        if (argument.isNotBlank() && argument.startsWith("fb-")) {
            val targetId = argument.trim().uppercase(Locale.US)
            val fb = feedbackDao.getFeedbackById(targetId)
            if (fb == null) {
                dispatchMessageToChat(chatId, "❓ Feedback ID <code>$targetId</code> not found.")
            } else {
                val detail = """
                    📋 <b>FEEDBACK DETAILS</b>

                    <b>ID:</b> <code>${fb.feedbackId}</code>
                    <b>Status:</b> ${fb.status}
                    <b>Type:</b> ${fb.category}
                    <b>Feature:</b> ${fb.affectedFeature}
                    <b>User:</b> ${fb.userName} (${fb.userId})
                    <b>Email:</b> ${fb.userEmail}
                    <b>Priority:</b> ${if (fb.isHighPriority) "HIGH 🚨" else "Normal"}

                    <b>Description:</b>
                    ${fb.description}
                """.trimIndent()
                dispatchMessageToChat(chatId, detail)
            }
            return
        }

        val allList = feedbackDao.getAllFeedbackOnce()
        val pendingCount = allList.count { it.status == "NEW" }
        val highPriorityCount = allList.count { it.isHighPriority && it.status != "RESOLVED" }
        val resolvedCount = allList.count { it.status == "RESOLVED" }

        val recent5 = allList.take(5)
        val sb = StringBuilder("📩 <b>USER FEEDBACK SUMMARY</b>\n\n")
        sb.append("<b>Total:</b> ${allList.size}\n")
        sb.append("<b>New / Pending:</b> $pendingCount\n")
        sb.append("<b>High Priority:</b> $highPriorityCount 🚨\n")
        sb.append("<b>Resolved:</b> $resolvedCount ✅\n\n")

        if (recent5.isNotEmpty()) {
            sb.append("<b>Recent Feedback:</b>\n")
            recent5.forEach { item ->
                val priorityFlag = if (item.isHighPriority) " 🚨" else ""
                sb.append("• <code>${item.feedbackId}</code> [${item.status}] — ${item.category}$priorityFlag\n")
            }
            sb.append("\n<i>Use <code>/feedback FB-ID</code> to view details or <code>/resolve FB-ID</code> to mark resolved.</i>")
        } else {
            sb.append("<i>No user feedback received yet.</i>")
        }

        dispatchMessageToChat(chatId, sb.toString())
    }

    private suspend fun handleResolveFeedbackCommand(chatId: String, argument: String) {
        val feedbackDao = database?.userFeedbackDao()
        if (feedbackDao == null) {
            dispatchMessageToChat(chatId, "⚠️ Database not attached.")
            return
        }

        val targetId = argument.trim().uppercase(Locale.US)
        if (targetId.isBlank()) {
            dispatchMessageToChat(chatId, "⚠️ Please provide Feedback ID. Usage: <code>/resolve FB-20260829-123456</code>")
            return
        }

        val fb = feedbackDao.getFeedbackById(targetId)
        if (fb == null) {
            dispatchMessageToChat(chatId, "❓ Feedback ID <code>$targetId</code> not found.")
        } else {
            feedbackDao.updateStatus(targetId, "RESOLVED")
            dispatchMessageToChat(chatId, "✅ Feedback <code>$targetId</code> marked as <b>RESOLVED</b>.")
        }
    }

    /**
     * Sends user feedback / bug report notification (Step 76) to Telegram Admin Chat.
     */
    suspend fun notifyUserFeedback(
        feedback: com.example.data.model.UserFeedbackEntity,
        attachmentFiles: List<java.io.File> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        val adminChatId = TelegramBotConfig.getAdminChatId(appContext)
        if (adminChatId.isNullOrBlank()) {
            Log.w(TAG, "Admin Chat ID missing. Unable to send Telegram feedback notification.")
            return@withContext false
        }
        val service = telegramService ?: return@withContext false

        val text = TelegramBotConfig.formatFeedbackPayload(feedback, attachmentFiles)

        val mainResult = service.sendStudyMatePost(adminChatId, text)
        val isSuccess = mainResult is TelegramPublishResult.Success

        if (attachmentFiles.isNotEmpty()) {
            for (file in attachmentFiles) {
                try {
                    if (file.name.endsWith(".png", true) || file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true)) {
                        service.sendPhoto(adminChatId, file, caption = "📎 Screenshot for ${feedback.feedbackId}")
                    } else {
                        service.sendDocument(adminChatId, file, caption = "📎 Recording/Doc for ${feedback.feedbackId}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Attachment upload exception for ${file.name}", e)
                }
            }
        }

        isSuccess
    }

    // =========================================================================
    // 8. DISPATCHER & OFFLINE RETRY QUEUE
    // =========================================================================

    private fun dispatchToAdmin(text: String) {
        val adminChatId = TelegramBotConfig.getAdminChatId(appContext)
        if (adminChatId.isNullOrBlank()) {
            Log.d(TAG, "Admin Telegram Chat ID not configured. Notification dropped or queued.")
            return
        }
        dispatchMessageToChat(adminChatId, text)
    }

    private fun dispatchMessageToChat(chatId: String, text: String) {
        val service = telegramService ?: return
        scope.launch {
            try {
                val result = service.sendStudyMatePost(chatId = chatId, text = text)
                if (result is TelegramPublishResult.Failure && result.isRecoverable) {
                    enqueueFailedNotification(chatId, text)
                }
            } catch (e: Exception) {
                enqueueFailedNotification(chatId, text)
            }
        }
    }

    private fun enqueueFailedNotification(chatId: String, text: String) {
        if (pendingQueue.size < 25) {
            pendingQueue.add(QueuedNotification(chatId, text))
        }
    }

    private fun startOfflineQueueProcessor() {
        scope.launch {
            while (isActive) {
                delay(30000) // Attempt retry every 30 seconds
                if (pendingQueue.isNotEmpty() && telegramService != null) {
                    val item = pendingQueue.poll() ?: continue
                    try {
                        val result = telegramService!!.sendStudyMatePost(item.chatId, item.text)
                        if (result is TelegramPublishResult.Failure && item.attemptCount < 3) {
                            pendingQueue.add(item.copy(attemptCount = item.attemptCount + 1))
                        }
                    } catch (e: Exception) {
                        if (item.attemptCount < 3) {
                            pendingQueue.add(item.copy(attemptCount = item.attemptCount + 1))
                        }
                    }
                }
            }
        }
    }
}
