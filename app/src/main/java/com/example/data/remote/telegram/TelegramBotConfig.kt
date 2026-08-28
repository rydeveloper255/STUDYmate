package com.example.data.remote.telegram

import com.example.BuildConfig

/**
 * Telegram Bot Configuration for StudyMate Official Bot.
 *
 * Security Note:
 * - The TELEGRAM_BOT_TOKEN is loaded exclusively via BuildConfig (injected from environment / secrets).
 * - The actual token is never committed, hardcoded, logged, or exposed in UI / API responses.
 */
object TelegramBotConfig {
    /**
     * Official StudyMate Telegram Bot Identification
     */
    const val BOT_ID: Long = 8684805634L
    const val BOT_USERNAME: String = "@StudyMateOfficialBot"
    const val BOT_DISPLAY_NAME: String = "Study mate"
    const val BASE_URL: String = "https://api.telegram.org/"

    /**
     * Regex pattern to detect and redact any accidental Telegram bot token strings in logs or exceptions.
     */
    private val TOKEN_REGEX = Regex("""\d{8,12}:[A-Za-z0-9_-]{30,45}""")

    /**
     * Safely reads the bot token from BuildConfig / Environment.
     * Returns null if missing, empty, or set to placeholder/dummy value.
     */
    fun getBotToken(): String? {
        val raw = try {
            BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        } catch (e: Throwable) {
            ""
        }
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
     * Returns true if a non-dummy token is configured.
     */
    fun isConfigured(): Boolean = getBotToken() != null

    /**
     * Returns a safe masked version of the token for diagnostics, e.g. "8684805***9dpVQ75yE" or "[NOT_CONFIGURED]".
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
     * Sanitizes any message, stack trace, or string to ensure bot tokens are never leaked in logs or error messages.
     */
    fun sanitize(message: String?): String {
        if (message == null) return "Unknown error"
        return TOKEN_REGEX.replace(message, "[REDACTED_TELEGRAM_TOKEN]")
    }
}
