package com.example.service.feedback

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.local.StudyMateDatabase
import com.example.data.model.FeedbackCategory
import com.example.data.model.UserFeedbackEntity
import com.example.data.model.UserProfile
import com.example.data.remote.telegram.TelegramBotConfig
import com.example.service.admin.TelegramAdminBotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Result model for attachment validation & local caching.
 */
data class ProcessedAttachment(
    val file: File,
    val originalName: String,
    val sizeBytes: Long,
    val isVideo: Boolean
)

/**
 * Centralized Manager for StudyMate User Feedback & Bug Report System (Step 76).
 */
object FeedbackManager {
    private const val TAG = "FeedbackManager"
    private const val MAX_ATTACHMENT_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB Limit
    private const val MIN_SUBMISSION_INTERVAL_MS = 10_000L // 10 seconds rate limit

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var appContext: Context? = null
    private var database: StudyMateDatabase? = null

    private val lastSubmissionTime = AtomicLong(0L)

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun initialize(context: Context, db: StudyMateDatabase) {
        appContext = context.applicationContext
        database = db
        // Kick off sync of pending feedback
        retryPendingFeedback()
    }

    /**
     * Generates a unique Feedback ID: FB-YYYYMMDD-XXXXXX
     */
    fun generateFeedbackId(): String {
        return TelegramFeedbackNotifier.generateFeedbackId()
    }

    /**
     * Processes selected attachment Uris (screenshots / screen recordings),
     * copies them to local cache directory, validates file sizes, and returns structured metadata.
     */
    suspend fun processAttachmentUris(
        context: Context,
        uris: List<Uri>
    ): Result<List<ProcessedAttachment>> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "feedback_attachments")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            var totalSizeBytes = 0L
            val resultList = mutableListOf<ProcessedAttachment>()

            for (uri in uris) {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: ""
                val isVideo = mimeType.contains("video", ignoreCase = true) || uri.toString().contains("video", ignoreCase = true)

                val fileName = "attachment_${System.currentTimeMillis()}_${resultList.size}.${if (isVideo) "mp4" else "png"}"
                val targetFile = File(cacheDir, fileName)

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null
                try {
                    inputStream = contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(Exception("Unable to read attachment file."))
                    outputStream = FileOutputStream(targetFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var fileSizeBytes = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        fileSizeBytes += bytesRead
                    }

                    totalSizeBytes += fileSizeBytes
                    if (totalSizeBytes > MAX_ATTACHMENT_SIZE_BYTES) {
                        targetFile.delete()
                        return@withContext Result.failure(
                            Exception("File size bahut badi hai. Kripya chhoti file select karein. (Max 25MB)")
                        )
                    }

                    resultList.add(
                        ProcessedAttachment(
                            file = targetFile,
                            originalName = fileName,
                            sizeBytes = fileSizeBytes,
                            isVideo = isVideo
                        )
                    )
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            }

            Result.success(resultList)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing attachments", e)
            Result.failure(Exception("Attachment process failed: ${e.message}"))
        }
    }

    /**
     * Submits new feedback / bug report.
     */
    suspend fun submitFeedback(
        context: Context,
        category: FeedbackCategory,
        title: String,
        description: String,
        affectedFeature: String,
        isHighPriority: Boolean,
        relatedErrorId: String?,
        attachments: List<ProcessedAttachment>
    ): Result<UserFeedbackEntity> = withContext(Dispatchers.IO) {
        if (_isSubmitting.value) {
            return@withContext Result.failure(Exception("Submission already in progress. Please wait."))
        }

        // Rate limiting check
        val now = System.currentTimeMillis()
        val elapsedSinceLast = now - lastSubmissionTime.get()
        if (elapsedSinceLast < MIN_SUBMISSION_INTERVAL_MS) {
            val waitSecs = ((MIN_SUBMISSION_INTERVAL_MS - elapsedSinceLast) / 1000) + 1
            return@withContext Result.failure(
                Exception("Please wait $waitSecs seconds before submitting another report.")
            )
        }

        if (description.isBlank()) {
            return@withContext Result.failure(Exception("Problem description cannot be empty. Detail me likhein."))
        }

        _isSubmitting.value = true
        try {
            lastSubmissionTime.set(now)

            val db = database ?: StudyMateDatabase.getDatabase(context)
            val activeUser: UserProfile? = try {
                db.userDao().getUserProfileOnce()
            } catch (e: Exception) { null }

            val userId = activeUser?.uid ?: "ANONYMOUS"
            val userName = activeUser?.name ?: "Student"
            val rawEmail = activeUser?.email ?: ""
            val maskedEmail = TelegramBotConfig.maskEmail(rawEmail)

            val appVersion = "1.0.1"
            val deviceModel = "${Build.MANUFACTURER.capitalize()} ${Build.MODEL}"
            val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

            val feedbackId = generateFeedbackId()

            val attachmentPaths = attachments.map { it.file.absolutePath }
            val attachmentPathsJson = if (attachmentPaths.isNotEmpty()) {
                JSONArray(attachmentPaths).toString()
            } else null

            val feedbackEntity = UserFeedbackEntity(
                feedbackId = feedbackId,
                category = category.name,
                title = title.ifBlank { category.label },
                description = description.trim(),
                affectedFeature = affectedFeature.ifBlank { "General" },
                isHighPriority = isHighPriority,
                relatedErrorId = relatedErrorId,
                userId = userId,
                userName = userName,
                userEmail = maskedEmail,
                appVersion = appVersion,
                deviceModel = deviceModel,
                androidVersion = androidVersion,
                status = "NEW",
                createdAtMillis = now,
                attachmentPathsJson = attachmentPathsJson,
                syncState = "PENDING"
            )

            // 1. Save locally in Room DB immediately (No data loss)
            db.userFeedbackDao().insertOrUpdateFeedback(feedbackEntity)

            // Ingest into Smart Intelligence Engine for correlation & issue clustering
            com.example.service.admin.StudyMateSmartIntelligenceEngine.ingestFeedback(
                feedbackId = feedbackEntity.feedbackId,
                category = feedbackEntity.category,
                feature = feedbackEntity.affectedFeature,
                description = feedbackEntity.description,
                isHighPriority = feedbackEntity.isHighPriority,
                relatedErrorId = feedbackEntity.relatedErrorId
            )

            // 2. Dispatch to Telegram Admin Bot asynchronously via TelegramFeedbackNotifier
            val attachmentFiles = attachments.map { it.file }
            val isDispatched = TelegramFeedbackNotifier.sendFeedbackToAdmin(
                context = context,
                feedback = feedbackEntity,
                attachmentFiles = attachmentFiles
            )

            val finalSyncState = if (isDispatched) "SENT" else "PENDING"
            db.userFeedbackDao().updateSyncState(feedbackId, finalSyncState)

            val updatedEntity = feedbackEntity.copy(syncState = finalSyncState)
            Log.i(TAG, "Successfully submitted feedback [$feedbackId], syncState: $finalSyncState")

            Result.success(updatedEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit feedback", e)
            Result.failure(Exception("Submission failed: ${e.message}"))
        } finally {
            _isSubmitting.value = false
        }
    }

    /**
     * Returns Flow of all submitted feedback for "My Feedback" history tab.
     */
    fun getAllFeedbackFlow(context: Context): Flow<List<UserFeedbackEntity>> {
        val db = database ?: StudyMateDatabase.getDatabase(context)
        return try {
            db.userFeedbackDao().getAllFeedbackFlow()
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    /**
     * Retries sending any pending offline feedback entries to Telegram.
     */
    fun retryPendingFeedback() {
        scope.launch {
            val db = database ?: return@launch
            try {
                val pendingList = db.userFeedbackDao().getPendingSyncFeedback()
                for (item in pendingList) {
                    val files = mutableListOf<File>()
                    if (!item.attachmentPathsJson.isNullOrBlank()) {
                        try {
                            val jsonArray = JSONArray(item.attachmentPathsJson)
                            for (i in 0 until jsonArray.length()) {
                                val path = jsonArray.getString(i)
                                val f = File(path)
                                if (f.exists()) {
                                    files.add(f)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing attachment paths for ${item.feedbackId}", e)
                        }
                    }

                    val success = TelegramAdminBotManager.notifyUserFeedback(
                        feedback = item,
                        attachmentFiles = files
                    )

                    if (success) {
                        db.userFeedbackDao().updateSyncState(item.feedbackId, "SENT")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Pending feedback retry encountered exception", e)
            }
        }
    }

    /**
     * Helper to format file size in KB or MB for display.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }
}
