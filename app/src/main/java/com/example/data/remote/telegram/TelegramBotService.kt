package com.example.data.remote.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Service layer for Telegram Bot connectivity, health verification, and credential validation.
 *
 * All operations run on Dispatchers.IO and guarantee that bot tokens are never logged or exposed.
 */
class TelegramBotService(
    private val apiService: TelegramApiService = TelegramBotClient.apiService
) {

    /**
     * Executes a health and credential check against the Telegram Bot API (via getMe).
     * Returns a structured [TelegramHealthStatus] representing the connection outcome.
     */
    suspend fun checkHealth(): TelegramHealthStatus = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val formattedTime = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", java.util.Locale.US).format(java.util.Date(now))

        val token = TelegramBotConfig.getBotToken()
        if (token == null) {
            return@withContext TelegramHealthStatus.Unconfigured(
                message = "TELEGRAM_BOT_TOKEN is missing or set to placeholder in environment.",
                lastCheckTimestamp = now,
                lastCheckFormatted = formattedTime
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val response = apiService.getMe(token)
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.ok && body.result != null) {
                    val user = body.result
                    return@withContext TelegramHealthStatus.Connected(
                        botId = user.id,
                        botUsername = if (user.username != null) "@${user.username}" else TelegramBotConfig.BOT_USERNAME,
                        botName = user.firstName,
                        canJoinGroups = user.canJoinGroups ?: true,
                        responseTimeMs = latency,
                        lastCheckTimestamp = now,
                        lastCheckFormatted = formattedTime
                    )
                } else {
                    val desc = TelegramBotConfig.sanitize(body?.description ?: "Telegram API returned ok=false")
                    return@withContext TelegramHealthStatus.InvalidCredentials(
                        errorCode = body?.errorCode ?: response.code(),
                        description = desc,
                        lastCheckTimestamp = now,
                        lastCheckFormatted = formattedTime
                    )
                }
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string()
                val sanitizedError = TelegramBotConfig.sanitize(errorBody ?: response.message())
                return@withContext if (code == 401 || code == 404) {
                    TelegramHealthStatus.InvalidCredentials(
                        errorCode = code,
                        description = "Unauthorized: The provided bot token is invalid or inactive.",
                        lastCheckTimestamp = now,
                        lastCheckFormatted = formattedTime
                    )
                } else {
                    TelegramHealthStatus.ConnectionError(
                        errorMessage = "HTTP $code: $sanitizedError",
                        lastCheckTimestamp = now,
                        lastCheckFormatted = formattedTime
                    )
                }
            }
        } catch (e: UnknownHostException) {
            return@withContext TelegramHealthStatus.ConnectionError(
                errorMessage = "Network error: Unable to resolve api.telegram.org. Please check internet connectivity.",
                lastCheckTimestamp = now,
                lastCheckFormatted = formattedTime
            )
        } catch (e: SocketTimeoutException) {
            return@withContext TelegramHealthStatus.ConnectionError(
                errorMessage = "Connection timeout: Telegram API server did not respond within 15 seconds.",
                lastCheckTimestamp = now,
                lastCheckFormatted = formattedTime
            )
        } catch (e: Exception) {
            return@withContext TelegramHealthStatus.ConnectionError(
                errorMessage = TelegramBotConfig.sanitize(e.message ?: "Unexpected error connecting to Telegram API"),
                lastCheckTimestamp = now,
                lastCheckFormatted = formattedTime
            )
        }
    }

    /**
     * Verifies the Telegram bot connection and returns the authenticated [TelegramUser] or an error.
     */
    suspend fun verifyConnection(): Result<TelegramUser> = withContext(Dispatchers.IO) {
        val token = TelegramBotConfig.getBotToken()
            ?: return@withContext Result.failure(
                IllegalStateException("Telegram Bot Token is not configured.")
            )

        try {
            val response = apiService.getMe(token)
            if (response.isSuccessful && response.body()?.ok == true && response.body()?.result != null) {
                Result.success(response.body()!!.result!!)
            } else {
                val desc = TelegramBotConfig.sanitize(response.body()?.description ?: "Failed to verify Telegram bot")
                Result.failure(Exception(desc))
            }
        } catch (e: Exception) {
            Result.failure(Exception(TelegramBotConfig.sanitize(e.message ?: "Failed to connect to Telegram API")))
        }
    }

    /**
     * Publishes a structured StudyMate post via Telegram Bot API.
     * Sanitizes errors to never reveal the bot token.
     */
    suspend fun sendStudyMatePost(
        chatId: String,
        text: String,
        parseMode: String = "HTML",
        disablePreview: Boolean = false,
        replyMarkup: InlineKeyboardMarkup? = null
    ): TelegramPublishResult = withContext(Dispatchers.IO) {
        val token = TelegramBotConfig.getBotToken()
            ?: return@withContext TelegramPublishResult.Failure(
                errorMessage = "Telegram Bot Token is not configured.",
                errorCode = 401,
                isRecoverable = false
            )

        try {
            val request = SendMessageRequest(
                chatId = chatId,
                text = text,
                parseMode = parseMode,
                disableWebPagePreview = disablePreview,
                replyMarkup = replyMarkup
            )
            val response = apiService.sendMessage(token, request)
            if (response.isSuccessful && response.body()?.ok == true && response.body()?.result != null) {
                val msg = response.body()!!.result!!
                TelegramPublishResult.Success(
                    messageId = msg.messageId,
                    targetChat = chatId
                )
            } else {
                val desc = TelegramBotConfig.sanitize(response.body()?.description ?: "Telegram API error: HTTP ${response.code()}")
                TelegramPublishResult.Failure(
                    errorMessage = desc,
                    errorCode = response.body()?.errorCode ?: response.code(),
                    isRecoverable = response.code() != 401 && response.code() != 403
                )
            }
        } catch (e: Exception) {
            val sanitized = TelegramBotConfig.sanitize(e.message ?: "Error dispatching Telegram post")
            TelegramPublishResult.Failure(
                errorMessage = sanitized,
                errorCode = null,
                isRecoverable = true
            )
        }
    }

    /**
     * Edits an existing Telegram message in-place with new text and optional reply markup.
     */
    suspend fun editMessageText(
        chatId: String,
        messageId: Long,
        text: String,
        parseMode: String = "HTML",
        disablePreview: Boolean = false,
        replyMarkup: InlineKeyboardMarkup? = null
    ): TelegramPublishResult = withContext(Dispatchers.IO) {
        val token = TelegramBotConfig.getBotToken()
            ?: return@withContext TelegramPublishResult.Failure(
                errorMessage = "Telegram Bot Token is not configured.",
                errorCode = 401,
                isRecoverable = false
            )

        try {
            val request = EditMessageTextRequest(
                chatId = chatId,
                messageId = messageId,
                text = text,
                parseMode = parseMode,
                disableWebPagePreview = disablePreview,
                replyMarkup = replyMarkup
            )
            val response = apiService.editMessageText(token, request)
            if (response.isSuccessful && response.body()?.ok == true && response.body()?.result != null) {
                TelegramPublishResult.Success(
                    messageId = response.body()!!.result!!.messageId,
                    targetChat = chatId
                )
            } else {
                val desc = TelegramBotConfig.sanitize(response.body()?.description ?: "Telegram editMessageText error: HTTP ${response.code()}")
                TelegramPublishResult.Failure(
                    errorMessage = desc,
                    errorCode = response.body()?.errorCode ?: response.code(),
                    isRecoverable = response.code() != 401 && response.code() != 403
                )
            }
        } catch (e: Exception) {
            val sanitized = TelegramBotConfig.sanitize(e.message ?: "Error editing Telegram message")
            TelegramPublishResult.Failure(
                errorMessage = sanitized,
                errorCode = null,
                isRecoverable = true
            )
        }
    }

    /**
     * Acknowledges a callback query from Telegram inline keyboard buttons.
     */
    suspend fun answerCallbackQuery(
        callbackQueryId: String,
        text: String? = null,
        showAlert: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val token = TelegramBotConfig.getBotToken() ?: return@withContext false
        try {
            val request = AnswerCallbackQueryRequest(
                callbackQueryId = callbackQueryId,
                text = text,
                showAlert = showAlert
            )
            val response = apiService.answerCallbackQuery(token, request)
            response.isSuccessful && response.body()?.ok == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sends a photo file to Telegram chat.
     */
    suspend fun sendPhoto(
        chatId: String,
        file: File,
        caption: String? = null
    ): TelegramPublishResult = withContext(Dispatchers.IO) {
        val token = TelegramBotConfig.getBotToken()
            ?: return@withContext TelegramPublishResult.Failure(
                errorMessage = "Telegram Bot Token is not configured.",
                errorCode = 401,
                isRecoverable = false
            )

        try {
            val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption?.toRequestBody("text/plain".toMediaTypeOrNull())
            val parseModeBody = "HTML".toRequestBody("text/plain".toMediaTypeOrNull())

            val mediaType = when {
                file.name.endsWith(".png", true) -> "image/png"
                file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) -> "image/jpeg"
                else -> "image/*"
            }.toMediaTypeOrNull()

            val requestFile = file.asRequestBody(mediaType)
            val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

            val response = apiService.sendPhoto(token, chatIdBody, captionBody, parseModeBody, photoPart)
            if (response.isSuccessful && response.body()?.ok == true && response.body()?.result != null) {
                TelegramPublishResult.Success(
                    messageId = response.body()!!.result!!.messageId,
                    targetChat = chatId
                )
            } else {
                val desc = TelegramBotConfig.sanitize(response.body()?.description ?: "Failed to upload photo")
                TelegramPublishResult.Failure(
                    errorMessage = desc,
                    errorCode = response.code(),
                    isRecoverable = true
                )
            }
        } catch (e: Exception) {
            TelegramPublishResult.Failure(
                errorMessage = TelegramBotConfig.sanitize(e.message ?: "Photo upload failed"),
                errorCode = null,
                isRecoverable = true
            )
        }
    }

    /**
     * Sends a document or video file to Telegram chat.
     */
    suspend fun sendDocument(
        chatId: String,
        file: File,
        caption: String? = null
    ): TelegramPublishResult = withContext(Dispatchers.IO) {
        val token = TelegramBotConfig.getBotToken()
            ?: return@withContext TelegramPublishResult.Failure(
                errorMessage = "Telegram Bot Token is not configured.",
                errorCode = 401,
                isRecoverable = false
            )

        try {
            val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption?.toRequestBody("text/plain".toMediaTypeOrNull())
            val parseModeBody = "HTML".toRequestBody("text/plain".toMediaTypeOrNull())

            val mediaType = when {
                file.name.endsWith(".mp4", true) -> "video/mp4"
                file.name.endsWith(".mkv", true) -> "video/x-matroska"
                file.name.endsWith(".pdf", true) -> "application/pdf"
                else -> "application/octet-stream"
            }.toMediaTypeOrNull()

            val requestFile = file.asRequestBody(mediaType)
            val docPart = MultipartBody.Part.createFormData("document", file.name, requestFile)

            val response = apiService.sendDocument(token, chatIdBody, captionBody, parseModeBody, docPart)
            if (response.isSuccessful && response.body()?.ok == true && response.body()?.result != null) {
                TelegramPublishResult.Success(
                    messageId = response.body()!!.result!!.messageId,
                    targetChat = chatId
                )
            } else {
                val desc = TelegramBotConfig.sanitize(response.body()?.description ?: "Failed to upload document")
                TelegramPublishResult.Failure(
                    errorMessage = desc,
                    errorCode = response.code(),
                    isRecoverable = true
                )
            }
        } catch (e: Exception) {
            TelegramPublishResult.Failure(
                errorMessage = TelegramBotConfig.sanitize(e.message ?: "Document upload failed"),
                errorCode = null,
                isRecoverable = true
            )
        }
    }

    /**
     * Polls for incoming updates from Telegram for authorized admin commands.
     */
    suspend fun getUpdates(offset: Long? = null, limit: Int = 20): Result<List<TelegramUpdate>> = withContext(Dispatchers.IO) {
        val token = TelegramBotConfig.getBotToken()
            ?: return@withContext Result.failure(IllegalStateException("Telegram Bot Token is not configured."))

        try {
            val response = apiService.getUpdates(token = token, offset = offset, limit = limit)
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(response.body()?.result ?: emptyList())
            } else {
                val desc = TelegramBotConfig.sanitize(response.body()?.description ?: "Failed to fetch updates")
                Result.failure(Exception(desc))
            }
        } catch (e: Exception) {
            Result.failure(Exception(TelegramBotConfig.sanitize(e.message ?: "Error polling updates")))
        }
    }
}

