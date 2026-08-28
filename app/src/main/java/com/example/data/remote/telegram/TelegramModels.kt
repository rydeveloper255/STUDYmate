package com.example.data.remote.telegram

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Standard Telegram Bot API generic response wrapper.
 */
@JsonClass(generateAdapter = true)
data class TelegramResponse<T>(
    @field:Json(name = "ok") val ok: Boolean,
    @field:Json(name = "result") val result: T? = null,
    @field:Json(name = "error_code") val errorCode: Int? = null,
    @field:Json(name = "description") val description: String? = null
)

/**
 * Telegram User / Bot entity representation returned by getMe.
 */
@JsonClass(generateAdapter = true)
data class TelegramUser(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "is_bot") val isBot: Boolean = true,
    @field:Json(name = "first_name") val firstName: String,
    @field:Json(name = "last_name") val lastName: String? = null,
    @field:Json(name = "username") val username: String? = null,
    @field:Json(name = "language_code") val languageCode: String? = null,
    @field:Json(name = "can_join_groups") val canJoinGroups: Boolean? = null,
    @field:Json(name = "can_read_all_group_messages") val canReadAllGroupMessages: Boolean? = null,
    @field:Json(name = "supports_inline_queries") val supportsInlineQueries: Boolean? = null
)

/**
 * Telegram Message object returned upon successful sendMessage.
 */
@JsonClass(generateAdapter = true)
data class TelegramMessage(
    @field:Json(name = "message_id") val messageId: Long,
    @field:Json(name = "date") val date: Long? = null,
    @field:Json(name = "text") val text: String? = null
)

/**
 * Request payload for sending a message via Telegram Bot API.
 */
@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @field:Json(name = "chat_id") val chatId: String,
    @field:Json(name = "text") val text: String,
    @field:Json(name = "parse_mode") val parseMode: String? = "HTML",
    @field:Json(name = "disable_web_page_preview") val disableWebPagePreview: Boolean = false
)

/**
 * Result wrapper for sending a Telegram message.
 */
sealed interface TelegramPublishResult {
    data class Success(
        val messageId: Long,
        val targetChat: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : TelegramPublishResult

    data class Failure(
        val errorMessage: String,
        val errorCode: Int? = null,
        val isRecoverable: Boolean = true
    ) : TelegramPublishResult
}

/**
 * Telegram Bot Connection & Health status sealed hierarchy.
 * Securely designed to never contain token strings.
 */
sealed interface TelegramHealthStatus {
    object Idle : TelegramHealthStatus
    object Checking : TelegramHealthStatus

    data class Connected(
        val botId: Long,
        val botUsername: String,
        val botName: String,
        val canJoinGroups: Boolean,
        val responseTimeMs: Long,
        val lastCheckTimestamp: Long = System.currentTimeMillis(),
        val lastCheckFormatted: String = ""
    ) : TelegramHealthStatus

    data class Unconfigured(
        val message: String = "TELEGRAM_BOT_TOKEN is not configured in environment or Secrets.",
        val lastCheckTimestamp: Long = System.currentTimeMillis(),
        val lastCheckFormatted: String = ""
    ) : TelegramHealthStatus

    data class InvalidCredentials(
        val errorCode: Int,
        val description: String,
        val lastCheckTimestamp: Long = System.currentTimeMillis(),
        val lastCheckFormatted: String = ""
    ) : TelegramHealthStatus

    data class ConnectionError(
        val errorMessage: String,
        val lastCheckTimestamp: Long = System.currentTimeMillis(),
        val lastCheckFormatted: String = ""
    ) : TelegramHealthStatus
}
