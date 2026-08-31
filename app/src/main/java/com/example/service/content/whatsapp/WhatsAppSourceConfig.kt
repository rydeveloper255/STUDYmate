package com.example.service.content.whatsapp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Step 82: Configuration for StudyMate Official WhatsApp Channel Content Source.
 * Centralized, configurable, and manageable through environment/backend settings.
 */
object WhatsAppSourceConfig {

    const val DEFAULT_CHANNEL_URL = "https://whatsapp.com/channel/0029VaAbQf01NCrYADMLt00L"
    const val SOURCE_TYPE = "WHATSAPP_CHANNEL"
    const val SOURCE_NAME = "StudyMate WhatsApp Updates"
    const val SOURCE_ID = "src_whatsapp_channel_updates"

    // Historical Start Date Cutoff: 1 August 2026
    const val HISTORICAL_CUTOFF_DATE_STR = "2026-08-01"

    private val IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata")

    @Volatile
    var configuredChannelUrl: String = DEFAULT_CHANNEL_URL

    @Volatile
    var historicalCutoffDate: String = HISTORICAL_CUTOFF_DATE_STR

    /**
     * Checks whether a given post date (in yyyy-MM-dd or parseable format) meets the 1 August 2026 cutoff rule.
     * Rule:
     * - source_post_date < 2026-08-01 -> false (IGNORE)
     * - source_post_date >= 2026-08-01 -> true (PROCESS)
     * - If date is null or invalid -> null (DATE_UNAVAILABLE)
     */
    fun isEligibleByCutoff(dateStr: String?): Boolean? {
        if (dateStr.isNullOrBlank()) return null

        val parsedDate = parseDateSafely(dateStr) ?: return null
        val cutoffDate = parseDateSafely(historicalCutoffDate) ?: return null

        return !parsedDate.before(cutoffDate)
    }

    /**
     * Safely parses multiple date representations into Date.
     */
    fun parseDateSafely(dateStr: String): Date? {
        val clean = dateStr.trim()
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "dd.MM.yyyy",
            "dd MMM yyyy",
            "dd MMMM yyyy",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                    timeZone = IST_TIMEZONE
                    isLenient = false
                }
                return sdf.parse(clean)
            } catch (_: Exception) {
                // Try next
            }
        }
        return null
    }

    /**
     * Formats Date to canonical ISO yyyy-MM-dd in IST.
     */
    fun formatDateToIso(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = IST_TIMEZONE
        }.format(date)
    }
}
