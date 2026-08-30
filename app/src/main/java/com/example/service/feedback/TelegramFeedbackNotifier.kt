package com.example.service.feedback

import android.content.Context
import android.util.Log
import com.example.data.model.UserFeedbackEntity
import com.example.data.remote.telegram.TelegramBotConfig
import com.example.service.admin.TelegramAdminBotManager
import java.io.File

/**
 * Utility to generate unique FB-XXXXXXXX IDs, format secure sanitized feedback payloads,
 * and dispatch notifications to the Telegram admin chat without exposing bot tokens.
 */
object TelegramFeedbackNotifier {
    private const val TAG = "TelegramFeedbackNotifier"

    /**
     * Generates a unique, standard Feedback ID format: FB-YYYYMMDD-XXXXXX.
     */
    fun generateFeedbackId(): String {
        return TelegramBotConfig.generateFeedbackId()
    }

    /**
     * Formats the feedback entity into a sanitized HTML payload for Telegram transmission.
     */
    fun formatPayload(
        feedback: UserFeedbackEntity,
        attachmentFiles: List<File> = emptyList()
    ): String {
        return TelegramBotConfig.formatFeedbackPayload(feedback, attachmentFiles)
    }

    /**
     * Asynchronously sends the formatted feedback payload and attachments to the Telegram Admin chat.
     * Retain security by retrieving tokens via BuildConfig/Configuration dynamically without exposing hardcoded credentials.
     */
    suspend fun sendFeedbackToAdmin(
        context: Context,
        feedback: UserFeedbackEntity,
        attachmentFiles: List<File> = emptyList()
    ): Boolean {
        return try {
            TelegramAdminBotManager.notifyUserFeedback(
                feedback = feedback,
                attachmentFiles = attachmentFiles
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send feedback notification to Telegram admin chat", e)
            false
        }
    }
}
