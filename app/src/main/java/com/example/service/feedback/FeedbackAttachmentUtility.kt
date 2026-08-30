package com.example.service.feedback

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility for securely handling, validating, and managing attachments (screenshots and screen recordings)
 * for the StudyMate Feedback & Bug Report System.
 */
object FeedbackAttachmentUtility {
    private const val TAG = "FeedbackAttachmentUtil"
    
    const val MAX_SINGLE_FILE_SIZE_BYTES = 20 * 1024 * 1024L // 20 MB per file
    const val MAX_TOTAL_ATTACHMENTS_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB total limit

    private val ALLOWED_IMAGE_MIME_TYPES = setOf(
        "image/png", "image/jpeg", "image/jpg", "image/webp"
    )

    private val ALLOWED_VIDEO_MIME_TYPES = setOf(
        "video/mp4", "video/x-matroska", "video/quicktime", "video/3gpp", "video/webm"
    )

    /**
     * Validates and copies selected Uris to the secure local application cache directory.
     * Prevents insecure public exposure, checks size bounds, and filters unsupported MIME types.
     */
    suspend fun processAndValidateUris(
        context: Context,
        uris: List<Uri>
    ): Result<List<ProcessedAttachment>> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "feedback_attachments")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val processedList = mutableListOf<ProcessedAttachment>()
            var accumulatedSizeBytes = 0L

            for (uri in uris) {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri)?.lowercase() ?: ""
                
                val isImage = ALLOWED_IMAGE_MIME_TYPES.any { mimeType.contains(it) } ||
                        uri.toString().contains("image", ignoreCase = true)
                val isVideo = ALLOWED_VIDEO_MIME_TYPES.any { mimeType.contains(it) } ||
                        uri.toString().contains("video", ignoreCase = true)

                if (!isImage && !isVideo) {
                    // Fallback extension check
                    val uriPath = uri.toString().lowercase()
                    val extensionValid = uriPath.endsWith(".png") || uriPath.endsWith(".jpg") ||
                            uriPath.endsWith(".jpeg") || uriPath.endsWith(".mp4")
                    if (!extensionValid) {
                        return@withContext Result.failure(
                            Exception("Unsupported file type. Please select PNG/JPG screenshots or MP4 screen recordings.")
                        )
                    }
                }

                val ext = if (isVideo) "mp4" else "png"
                val sanitizedFileName = "feedback_attach_${System.currentTimeMillis()}_${processedList.size + 1}.$ext"
                val targetFile = File(cacheDir, sanitizedFileName)

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null
                try {
                    inputStream = contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(Exception("Unable to read selected attachment file."))

                    outputStream = FileOutputStream(targetFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var singleFileSizeBytes = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        singleFileSizeBytes += bytesRead

                        if (singleFileSizeBytes > MAX_SINGLE_FILE_SIZE_BYTES) {
                            targetFile.delete()
                            return@withContext Result.failure(
                                Exception("File size too large. Individual attachment must be under 20MB.")
                            )
                        }
                    }

                    accumulatedSizeBytes += singleFileSizeBytes
                    if (accumulatedSizeBytes > MAX_TOTAL_ATTACHMENTS_SIZE_BYTES) {
                        targetFile.delete()
                        return@withContext Result.failure(
                            Exception("File size bahut badi hai. Kripya chhoti file select karein. (Max 25MB total)")
                        )
                    }

                    processedList.add(
                        ProcessedAttachment(
                            file = targetFile,
                            originalName = sanitizedFileName,
                            sizeBytes = singleFileSizeBytes,
                            isVideo = isVideo
                        )
                    )
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            }

            Result.success(processedList)
        } catch (e: Exception) {
            Log.e(TAG, "Error in processAndValidateUris", e)
            Result.failure(Exception("Attachment processing failed: ${e.message}"))
        }
    }

    /**
     * Deletes temporary attachment files after successful transmission or dialog dismissal.
     */
    fun cleanupTempFiles(attachments: List<ProcessedAttachment>) {
        try {
            for (item in attachments) {
                if (item.file.exists()) {
                    val deleted = item.file.delete()
                    Log.d(TAG, "Cleanup temp file ${item.file.name}: deleted=$deleted")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup exception", e)
        }
    }

    /**
     * Clears all old temporary attachment files in the cache directory.
     */
    fun clearCacheDirectory(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "feedback_attachments")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000L) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache dir clear exception", e)
        }
    }
}
