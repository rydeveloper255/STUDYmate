package com.example.data.remote.telegram

import android.content.Context
import com.example.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Telegram Bot Configuration for StudyMate Official Bot & Admin Monitoring.
 *
 * Security Note:
 * - The TELEGRAM_BOT_TOKEN and TELEGRAM_ADMIN_CHAT_ID are loaded via BuildConfig / SharedPreferences.
 * - The actual token is never committed, hardcoded, logged, or exposed in UI / API responses.
 * - All outbound messages and logs are strictly sanitized to redact tokens, passwords, and sensitive keys.
 */
object TelegramBotConfig {
    /**
     * Official StudyMate Telegram Bot Identification
     */
    const val BOT_ID: Long = 8684805634L
    const val BOT_USERNAME: String = "@StudyMateOfficialBot"
    const val BOT_DISPLAY_NAME: String = "Study mate"
    const val BASE_URL: String = "https://api.telegram.org/"

    private const val PREFS_NAME = "studymate_admin_telegram_prefs"
    private const val KEY_ADMIN_CHAT_ID = "admin_telegram_chat_id"

    private val errorCounter = AtomicInteger((1000..9999).random())
    private val eventCounter = AtomicInteger((1000..9999).random())
    private val feedbackCounter = AtomicInteger((1000..9999).random())

    /**
     * Regex patterns to detect and redact sensitive credentials from logs or Telegram messages.
     */
    private val TOKEN_REGEX = Regex("""\d{8,12}:[A-Za-z0-9_-]{30,45}""")
    private val JWT_REGEX = Regex("""eyJ[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+\.?[A-Za-z0-9-_.+/=]*""")
    private val SECRET_PARAM_REGEX = Regex("""(?i)(password|pass|token|secret|api_key|anon_key|otp|bearer|authorization)=([^\s&]+)""")

    private fun getBuildConfigField(fieldName: String): String {
        return try {
            val field = BuildConfig::class.java.getField(fieldName)
            (field.get(null) as? String)?.trim() ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Safely reads the bot token from BuildConfig / Environment.
     * Returns null if missing, empty, or set to placeholder/dummy value.
     */
    fun getBotToken(): String? {
        val raw = getBuildConfigField("TELEGRAM_BOT_TOKEN")
        return if (raw.isBlank() ||
            raw.equals("dummy_telegram_bot_token", ignoreCase = true) ||
            raw.equals("dummy_key", ignoreCase = true) ||
            raw.startsWith("YOUR_", ignoreCase = true) ||
            raw.contains("placeholder", ignoreCase = true)
        ) {
            null
        } else {
            raw
        }
    }

    /**
     * Returns the configured Admin Chat ID.
     * Looks up SharedPreferences first (if user/admin set it at runtime), then BuildConfig.
     */
    fun getAdminChatId(context: Context? = null): String? {
        if (context != null) {
            val custom = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ADMIN_CHAT_ID, null)?.trim()
            if (!custom.isNullOrBlank()) {
                return custom
            }
        }
        val buildId = getBuildConfigField("TELEGRAM_ADMIN_CHAT_ID")
        return if (buildId.isNotBlank() && !buildId.contains("dummy", ignoreCase = true)) {
            buildId
        } else {
            null
        }
    }

    /**
     * Stores an authorized Admin Chat ID dynamically.
     */
    fun setAdminChatId(context: Context, chatId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ADMIN_CHAT_ID, chatId.trim())
            .apply()
    }

    /**
     * Validates whether an incoming Telegram chat ID belongs to an authorized administrator.
     */
    fun isAuthorizedAdmin(chatId: String, context: Context? = null): Boolean {
        val configuredAdminId = getAdminChatId(context) ?: return false
        return configuredAdminId.trim() == chatId.trim()
    }

    /**
     * Returns true if a valid bot token is configured.
     */
    fun isConfigured(): Boolean = getBotToken() != null

    /**
     * Returns a safe masked version of the token for diagnostics.
     * Never reveals the complete token.
     */
    fun getMaskedToken(): String {
        val token = getBotToken() ?: return "[NOT_CONFIGURED]"
        return if (token.length > 10) {
            "${token.take(6)}...${token.takeLast(4)}"
        } else {
            "[CONFIGURED]"
        }
    }

    /**
     * Masks user emails for privacy (e.g., "ryadav810232@gmail.com" -> "r***2@gmail.com").
     */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return "Not provided"
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) return "***@${email.substringAfter('@', "domain.com")}"
        val username = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        val maskedUser = if (username.length <= 2) {
            "${username.first()}***"
        } else {
            "${username.first()}***${username.last()}"
        }
        return "$maskedUser$domain"
    }

    /**
     * Generates a unique, standard Feedback ID format: FB-YYYYMMDD-XXXXXX.
     */
    fun generateFeedbackId(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val count = feedbackCounter.incrementAndGet() % 1000000
        return "FB-$dateStr-${String.format(Locale.US, "%06d", count)}"
    }

    /**
     * Formats a UserFeedbackEntity into a clean, sanitized HTML payload for Telegram Admin notifications.
     */
    fun formatFeedbackPayload(
        feedback: com.example.data.model.UserFeedbackEntity,
        attachmentFiles: List<java.io.File> = emptyList()
    ): String {
        val header = if (feedback.isHighPriority) {
            "🚨 <b>HIGH PRIORITY USER REPORT</b>"
        } else {
            "📩 <b>NEW USER FEEDBACK</b>"
        }

        val categoryObj = com.example.data.model.FeedbackCategory.fromString(feedback.category)
        val categoryLabel = "${categoryObj.iconEmoji} ${categoryObj.label}"
        val createdTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(feedback.createdAtMillis))

        val errorLink = if (!feedback.relatedErrorId.isNullOrBlank()) {
            "\n<b>Related Error ID:</b> <code>${sanitize(feedback.relatedErrorId)}</code>"
        } else ""

        val attachmentSummary = if (attachmentFiles.isNotEmpty()) {
            val photosCount = attachmentFiles.count {
                it.name.endsWith(".png", true) || it.name.endsWith(".jpg", true) || it.name.endsWith(".jpeg", true)
            }
            val videosCount = attachmentFiles.size - photosCount
            "📸 $photosCount Photo(s), 🎥 $videosCount Recording(s)"
        } else {
            "None"
        }

        return """
            $header

            <b>Feedback ID:</b> <code>${sanitize(feedback.feedbackId)}</code>
            <b>Type:</b> $categoryLabel
            <b>Feature:</b> ${sanitize(feedback.affectedFeature)}$errorLink

            <b>User:</b> ${sanitize(feedback.userName)}
            <b>User ID:</b> <code>${sanitize(feedback.userId)}</code>
            <b>Email:</b> ${sanitize(feedback.userEmail.ifBlank { "Not Provided" })}
            <b>App Version:</b> ${sanitize(feedback.appVersion)}
            <b>Device:</b> ${sanitize(feedback.deviceModel)}
            <b>OS:</b> ${sanitize(feedback.androidVersion)}

            <b>Title:</b> ${sanitize(feedback.title)}
            <b>Description:</b>
            ${sanitize(feedback.description)}

            <b>Attachments:</b> $attachmentSummary
            <b>Time:</b> $createdTime
            <b>Status:</b> ${sanitize(feedback.status)}
        """.trimIndent()
    }

    /**
     * Generates a unique, standard Error ID format: ERR-YYYYMMDD-XXXXXX.
     */
    fun generateErrorId(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val count = errorCounter.incrementAndGet() % 1000000
        return "ERR-$dateStr-${String.format(Locale.US, "%06d", count)}"
    }

    /**
     * Generates a unique, standard Event ID format: EVT-YYYYMMDD-XXXXXX.
     */
    fun generateEventId(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val count = eventCounter.incrementAndGet() % 1000000
        return "EVT-$dateStr-${String.format(Locale.US, "%06d", count)}"
    }

    /**
     * Sanitizes any message, stack trace, or string to ensure bot tokens, JWTs, passwords,
     * OTPs, and private keys are never leaked.
     */
    fun sanitize(message: String?): String {
        if (message == null) return "Unknown error"
        var clean = message
        clean = TOKEN_REGEX.replace(clean, "[REDACTED_TELEGRAM_TOKEN]")
        clean = JWT_REGEX.replace(clean, "[REDACTED_JWT_TOKEN]")
        clean = SECRET_PARAM_REGEX.replace(clean, "$1=[REDACTED]")
        return clean
    }
}
