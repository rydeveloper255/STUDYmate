package com.example.service.intelligence.smart

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * Step 83: Smart Date Intelligence.
 * 
 * Handles multi-format date extraction, normalization to ISO (yyyy-MM-dd),
 * impossible date validation, date extension detection, and vacancy expiry computation.
 */
object SmartDateIntelligence {

    private val IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata")
    const val HISTORICAL_CUTOFF_ISO = "2026-08-01"

    private val DATE_REGEX = "(?:[0-9]{4}[-/.][0-9]{1,2}[-/.][0-9]{1,2}|[0-9]{1,2}[-/.][0-9]{1,2}[-/.][0-9]{2,4}|[0-9]{1,2}\\s+[A-Za-z]+\\s+[0-9]{4}|[A-Za-z]+\\s+[0-9]{1,2},?\\s+[0-9]{4})"

    private val LAST_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:last\\s*date\\s*extended|extended\\s*(?:last\\s*date|to|till)|बढ़ाकर|आवेदन\\s*की\\s*अंतिम\\s*तिथि\\s*बढ़ाकर)[:\\s]*($DATE_REGEX)"),
        Pattern.compile("(?i)(?:last\\s*date|apply\\s*(?:online\\s*)?till|apply\\s*till|closing\\s*date|deadline|अंतिम\\s*तिथि|आवेदन\\s*की\\s*अंतिम\\s*तिथि)[:\\s]*($DATE_REGEX)")
    )

    private val START_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:start\\s*date|apply\\s*start|opening\\s*date|आवेदन\\s*शुरू|जारी\\s*तिथि|published\\s*date)[:\\s]*($DATE_REGEX)")
    )

    private val EXAM_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:exam\\s*date|cbt\\s*date|examination\\s*date|परीक्षा\\s*तिथि|परीक्षा\\s*दिनांक)[:\\s]*($DATE_REGEX|[A-Za-z]+\\s+[0-9]{4})")
    )

    private val RESULT_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:result\\s*date|declared\\s*on|रिजल्ट\\s*तिथि|परिणाम\\s*दिनांक)[:\\s]*($DATE_REGEX)")
    )

    private val ADMIT_CARD_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:admit\\s*card\\s*date|released\\s*on|available\\s*from|एडमिट\\s*कार्ड\\s*जारी)[:\\s]*($DATE_REGEX)")
    )

    private val ANSWER_KEY_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:answer\\s*key\\s*date|objection\\s*till|released\\s*on|उत्तर\\s*कुंजी\\s*जारी)[:\\s]*($DATE_REGEX)")
    )

    private val ADMISSION_DATE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:admission\\s*date|counseling\\s*date|allotment\\s*date|प्रवेश\\s*तिथि)[:\\s]*($DATE_REGEX)")
    )

    private val EXTENSION_KEYWORD_PATTERNS = listOf(
        Pattern.compile("(?i)(?:last\\s*date\\s*extended|extended\\s*last\\s*date|application\\s*date\\s*extended|extended\\s*to|बढ़ाकर)"),
    )

    private val SUPPORTED_DATE_FORMATS = listOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd.MM.yyyy",
        "d MMMM yyyy",
        "d MMM yyyy",
        "MMMM d, yyyy",
        "MMM d, yyyy",
        "yyyy/MM/dd",
        "dd/MM/yy",
        "dd-MM-yy"
    )

    data class ExtractedDates(
        val startDate: String? = null,
        val lastDate: String? = null,
        val examDate: String? = null,
        val resultDate: String? = null,
        val admitCardDate: String? = null,
        val answerKeyDate: String? = null,
        val admissionDate: String? = null,
        val isLastDateExtended: Boolean = false,
        val originalLastDate: String? = null,
        val invalidDateDetected: Boolean = false
    )

    /**
     * Extracts and normalizes all relevant dates from text context.
     */
    fun extractAllDates(text: String, postDate: String?): ExtractedDates {
        var invalidFound = false

        val rawStart = extractFirstMatchingDate(text, START_DATE_PATTERNS)
        val normStart = rawStart?.let { 
            normalizeToIso(it) ?: run { invalidFound = true; null }
        } ?: postDate?.let { normalizeToIso(it) }

        // Check if there is an extended last date mention
        val isExtension = EXTENSION_KEYWORD_PATTERNS.any { it.matcher(text).find() }
        
        // Find all candidate last dates in text
        val rawLast = extractFirstMatchingDate(text, LAST_DATE_PATTERNS)
        val normLast = rawLast?.let {
            normalizeToIso(it) ?: run { invalidFound = true; null }
        }

        val rawExam = extractFirstMatchingDate(text, EXAM_DATE_PATTERNS)
        val normExam = rawExam?.let {
            normalizeToIso(it) ?: it // Could be textual month e.g. "October 2026"
        }

        val rawResult = extractFirstMatchingDate(text, RESULT_DATE_PATTERNS)
        val normResult = rawResult?.let {
            normalizeToIso(it) ?: run { invalidFound = true; null }
        }

        val rawAdmit = extractFirstMatchingDate(text, ADMIT_CARD_DATE_PATTERNS)
        val normAdmit = rawAdmit?.let {
            normalizeToIso(it) ?: run { invalidFound = true; null }
        }

        val rawAnswerKey = extractFirstMatchingDate(text, ANSWER_KEY_DATE_PATTERNS)
        val normAnswerKey = rawAnswerKey?.let {
            normalizeToIso(it) ?: run { invalidFound = true; null }
        }

        val rawAdmission = extractFirstMatchingDate(text, ADMISSION_DATE_PATTERNS)
        val normAdmission = rawAdmission?.let {
            normalizeToIso(it) ?: run { invalidFound = true; null }
        }

        return ExtractedDates(
            startDate = normStart,
            lastDate = normLast,
            examDate = normExam,
            resultDate = normResult,
            admitCardDate = normAdmit,
            answerKeyDate = normAnswerKey,
            admissionDate = normAdmission,
            isLastDateExtended = isExtension,
            invalidDateDetected = invalidFound
        )
    }

    private fun extractFirstMatchingDate(text: String, patterns: List<Pattern>): String? {
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val group = matcher.group(1)?.trim()
                if (!group.isNullOrBlank()) {
                    return group
                }
            }
        }
        return null
    }

    /**
     * Parses and converts any supported date format into canonical ISO (yyyy-MM-dd).
     * Strictly verifies calendar validity (e.g. rejects 32 August, 30 Feb, etc.).
     */
    fun normalizeToIso(dateStr: String): String? {
        val trimmed = dateStr.trim().trimEnd('.', ',', ';', ')')
        if (trimmed.isBlank()) return null

        // Impossible date pre-check
        val dmyPattern = Pattern.compile("^([0-9]{1,2})[-/.]([0-9]{1,2})[-/.]([0-9]{2,4})$")
        val ymdPattern = Pattern.compile("^([0-9]{4})[-/.]([0-9]{1,2})[-/.]([0-9]{1,2})$")
        
        val mDmy = dmyPattern.matcher(trimmed)
        if (mDmy.matches()) {
            val d = mDmy.group(1)?.toIntOrNull() ?: 0
            val month = mDmy.group(2)?.toIntOrNull() ?: 0
            if (d < 1 || d > 31 || month < 1 || month > 12) {
                return null // Impossible calendar date
            }
        }
        val mYmd = ymdPattern.matcher(trimmed)
        if (mYmd.matches()) {
            val month = mYmd.group(2)?.toIntOrNull() ?: 0
            val d = mYmd.group(3)?.toIntOrNull() ?: 0
            if (d < 1 || d > 31 || month < 1 || month > 12) {
                return null // Impossible calendar date
            }
        }

        for (pattern in SUPPORTED_DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false // STRICT calendar verification!
                    timeZone = IST_TIMEZONE
                }
                val parsed = sdf.parse(trimmed)
                if (parsed != null) {
                    val outSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                        timeZone = IST_TIMEZONE
                    }
                    val formatted = outSdf.format(parsed)
                    
                    // Double check parsed year is reasonable (e.g. 2024..2035)
                    val cal = Calendar.getInstance(IST_TIMEZONE).apply { time = parsed }
                    val year = cal.get(Calendar.YEAR)
                    if (year in 2024..2035) {
                        return formatted
                    }
                }
            } catch (e: Exception) {
                // Continue trying next format
            }
        }

        return null
    }

    /**
     * Evaluates if a given date string is valid and not impossible.
     */
    fun isValidDate(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        return normalizeToIso(dateStr) != null
    }

    /**
     * Checks if a last date is before today in IST.
     */
    fun isDateExpired(lastDateIso: String?, todayIso: String = getTodayIso()): Boolean {
        if (lastDateIso.isNullOrBlank()) return false
        val lastDate = parseIsoDate(lastDateIso) ?: return false
        val today = parseIsoDate(todayIso) ?: return false
        return lastDate.before(today)
    }

    /**
     * Checks if post date is eligible by historical cutoff (>= 2026-08-01).
     * Returns true if eligible, false if before cutoff, null if date unavailable.
     */
    fun isEligibleByCutoff(postDateStr: String?): Boolean? {
        if (postDateStr.isNullOrBlank()) return null
        val normalized = normalizeToIso(postDateStr) ?: return null
        val postDate = parseIsoDate(normalized) ?: return null
        val cutoff = parseIsoDate(HISTORICAL_CUTOFF_ISO) ?: return null
        return !postDate.before(cutoff)
    }

    fun getTodayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = IST_TIMEZONE
        }
        return sdf.format(Date())
    }

    private fun parseIsoDate(isoStr: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                timeZone = IST_TIMEZONE
            }.parse(isoStr)
        } catch (e: Exception) {
            null
        }
    }
}
