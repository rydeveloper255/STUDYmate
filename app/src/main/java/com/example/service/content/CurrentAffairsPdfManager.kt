package com.example.service.content

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.content.WeeklyCurrentAffairsPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object CurrentAffairsPdfManager {
    private const val TAG = "CurrentAffairsPdfMgr"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 StudyMateApp/1.0"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun getPdfDirectory(context: Context): File {
        val dir = File(context.cacheDir, "ca_pdfs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks if local cache contains a valid PDF for this record.
     */
    fun getLocalPdfFile(context: Context, pdfId: String): File? {
        val file = File(getPdfDirectory(context), "$pdfId.pdf")
        if (file.exists() && file.length() > 100 && isPdfHeaderValid(file)) {
            return file
        }
        return null
    }

    /**
     * Retrieves or downloads the PDF file locally without calling external browser.
     */
    suspend fun getOrDownloadPdfFile(
        context: Context,
        pdf: WeeklyCurrentAffairsPdf,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val cached = getLocalPdfFile(context, pdf.id)
        if (cached != null) {
            Log.d(TAG, "Using locally cached PDF for ${pdf.id}")
            return@withContext Result.success(cached)
        }

        // Try downloading from pdfPublicUrl, pdfSourceUrl, or storage path
        val downloadUrls = listOfNotNull(
            pdf.pdfPublicUrl.takeIf { !it.isNullOrBlank() },
            pdf.pdfSourceUrl.takeIf { !it.isNullOrBlank() && !it.endsWith("#") && !it.contains("#week-") }
        ).ifEmpty { listOf(pdf.pdfSourceUrl) }

        var lastException: Exception? = null

        for (targetUrl in downloadUrls) {
            try {
                Log.d(TAG, "Attempting PDF download from: $targetUrl")
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/pdf,application/octet-stream,*/*")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP error ${response.code} downloading PDF")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()

                val tempFile = File(getPdfDirectory(context), "${pdf.id}_temp.pdf")
                val outputStream = FileOutputStream(tempFile)
                val inputStream: InputStream = body.byteStream()

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0 && onProgress != null) {
                        val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (!isPdfHeaderValid(tempFile)) {
                    tempFile.delete()
                    throw Exception("Downloaded content is not a valid PDF file")
                }

                val finalFile = File(getPdfDirectory(context), "${pdf.id}.pdf")
                if (finalFile.exists()) finalFile.delete()
                tempFile.renameTo(finalFile)

                Log.i(TAG, "Successfully downloaded PDF to ${finalFile.absolutePath} (${finalFile.length()} bytes)")
                return@withContext Result.success(finalFile)

            } catch (e: Exception) {
                Log.w(TAG, "Failed download attempt from $targetUrl: ${e.message}")
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Could not download PDF from available URLs"))
    }

    /**
     * Checks if the file starts with the %PDF magic byte header.
     */
    private fun isPdfHeaderValid(file: File): Boolean {
        return try {
            val bytes = ByteArray(4)
            val fis = file.inputStream()
            val read = fis.read(bytes)
            fis.close()
            read == 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte() // %PDF
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Saves the cached PDF to the device's public Downloads directory.
     */
    suspend fun savePdfToDownloads(
        context: Context,
        pdf: WeeklyCurrentAffairsPdf,
        localFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanDate = pdf.dateRange.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "Weekly" }
            val fileName = "StudyMate_Current_Affairs_${cleanDate}_Hindi.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudyMate")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Could not create Downloads MediaStore entry")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    localFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Result.success("Saved to Downloads/StudyMate/$fileName")
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val studyMateDir = File(downloadsDir, "StudyMate")
                if (!studyMateDir.exists()) studyMateDir.mkdirs()

                val targetFile = File(studyMateDir, fileName)
                localFile.copyTo(targetFile, overwrite = true)
                Result.success("Saved to Downloads/StudyMate/$fileName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving PDF to downloads", e)
            Result.failure(e)
        }
    }

    /**
     * Shares the PDF file via Android Share Sheet.
     */
    fun sharePdfFile(context: Context, pdf: WeeklyCurrentAffairsPdf, localFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                localFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, pdf.title)
                putExtra(Intent.EXTRA_TEXT, "${pdf.title}\nStudyMate Current Affairs (${pdf.dateRange})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Current Affairs PDF"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share PDF", e)
        }
    }
}
