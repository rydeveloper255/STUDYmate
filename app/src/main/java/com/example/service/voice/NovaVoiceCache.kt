package com.example.service.voice

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * High-performance deterministic audio cache for NOVA Voice Synthesis.
 *
 * Prevents redundant ElevenLabs API calls and reduces latency for repeated questions,
 * common greetings, test previews, and daily study reminders.
 *
 * Cache keys are derived deterministically from:
 * SHA256(normalizedText + "|" + voiceId + "|" + modelId + "|" + speed + "|" + language)
 */
class NovaVoiceCache(private val context: Context) {

    private val cacheDir: File by lazy {
        val dir = File(context.cacheDir, "nova_voice_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    companion object {
        private const val MAX_CACHE_SIZE_BYTES = 50L * 1024L * 1024L // 50 MB
        private const val MAX_FILE_COUNT = 150
    }

    /**
     * Generates a unique, deterministic SHA-256 hash key for given TTS parameters.
     */
    fun createCacheKey(
        normalizedText: String,
        voiceId: String,
        modelId: String,
        speed: Float,
        language: String
    ): String {
        val input = "${normalizedText.trim()}|$voiceId|$modelId|${String.format("%.2f", speed)}|$language"
        return sha256(input)
    }

    /**
     * Checks if a cached MP3 file exists and is readable.
     */
    fun getAudioFile(cacheKey: String): File? {
        val file = File(cacheDir, "$cacheKey.mp3")
        return if (file.exists() && file.length() > 0) {
            // Update last modified for LRU eviction policy
            file.setLastModified(System.currentTimeMillis())
            file
        } else {
            null
        }
    }

    /**
     * Saves raw MP3 audio bytes to cache file.
     */
    fun saveAudio(cacheKey: String, audioBytes: ByteArray): File? {
        if (audioBytes.isEmpty()) return null
        return try {
            pruneCacheIfNeeded()
            val file = File(cacheDir, "$cacheKey.mp3")
            FileOutputStream(file).use { out ->
                out.write(audioBytes)
                out.flush()
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears all cached voice files (e.g. when voice personality or settings are reset).
     */
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Prunes oldest cached audio files when cache exceeds size or count limits.
     */
    private fun pruneCacheIfNeeded() {
        try {
            val files = cacheDir.listFiles() ?: return
            var totalSize = files.sumOf { it.length() }

            if (totalSize > MAX_CACHE_SIZE_BYTES || files.size > MAX_FILE_COUNT) {
                val sortedFiles = files.sortedBy { it.lastModified() }
                for (f in sortedFiles) {
                    val len = f.length()
                    if (f.delete()) {
                        totalSize -= len
                    }
                    if (totalSize < MAX_CACHE_SIZE_BYTES * 0.7 && files.size < MAX_FILE_COUNT * 0.7) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // Non-critical cache cleanup failure
        }
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
