package com.example.service.intelligence.verification

import com.example.service.intelligence.smart.SmartContentCategory
import com.example.service.intelligence.smart.SmartDateIntelligence
import com.example.service.intelligence.smart.SmartExtractedData
import com.example.service.intelligence.smart.SmartLink
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Step 84: Layer 1 — Deterministic Validation Engine.
 * 
 * Executes mathematical, algorithmic, syntactic, and structural validation checks
 * without making AI calls:
 * - Date format and calendar validity (32 August, 31 February rejection)
 * - Safe URL syntax and dangerous scheme rejection (javascript:, data:, file:, intent:)
 * - Category-specific mandatory completeness
 * - Expiry calculation against current IST date
 * - Historical date cutoff (>= 2026-08-01)
 * - Database and model invariant constraints
 */
object DeterministicValidator {

    private val IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata")
    private val BLOCKED_SCHEMES = setOf(
        "javascript", "data", "file", "intent", "blob", "vbscript", "about", "tel", "sms", "mailto"
    )
    private val ALLOWED_SCHEMES = setOf("http", "https")

    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>,
        val flaggedReasons: List<ReviewReason>,
        val isExpired: Boolean = false,
        val isCutoffEligible: Boolean = true
    )

    /**
     * Performs complete Layer 1 deterministic validation.
     */
    fun validate(
        title: String,
        category: SmartContentCategory,
        extractedData: SmartExtractedData,
        sourcePostDate: String?,
        lastDate: String?,
        examDate: String?,
        links: List<SmartLink>,
        sourceMessageId: String? = null
    ): ValidationResult {
        val issues = mutableListOf<String>()
        val reasons = mutableListOf<ReviewReason>()

        // 1. Mandatory Title and ID constraints
        if (title.isBlank() || title.equals("Official Update", ignoreCase = true)) {
            issues.add("Title is missing or is generic placeholder")
            reasons.add(ReviewReason.MISSING_CRITICAL_DATA)
        }

        // 2. Cutoff Check (1 August 2026 rule)
        var isCutoffEligible = true
        if (!sourcePostDate.isNullOrBlank()) {
            val cutoff = SmartDateIntelligence.isEligibleByCutoff(sourcePostDate)
            if (cutoff == false) {
                issues.add("Source post date '$sourcePostDate' is strictly before 1 August 2026 cutoff")
                isCutoffEligible = false
            }
        }

        // 3. Calendar Date Sanity & Format Checks
        val datesToCheck = listOfNotNull(
            sourcePostDate?.let { "post_date" to it },
            lastDate?.let { "last_date" to it },
            examDate?.let { "exam_date" to it },
            when (extractedData) {
                is SmartExtractedData.Vacancy -> extractedData.data.applicationStartDate?.let { "application_start_date" to it }
                is SmartExtractedData.Result -> extractedData.data.resultDate?.let { "result_date" to it }
                is SmartExtractedData.AdmitCard -> extractedData.data.admitCardReleaseDate?.let { "admit_card_date" to it }
                is SmartExtractedData.AnswerKey -> extractedData.data.answerKeyDate?.let { "answer_key_date" to it }
                is SmartExtractedData.Admission -> extractedData.data.applicationStartDate?.let { "application_start_date" to it }
                else -> null
            }
        )

        for ((field, dateStr) in datesToCheck) {
            val dateValidation = validateCalendarDate(dateStr)
            if (!dateValidation.isValid) {
                issues.add("Invalid calendar date in field '$field': $dateStr (${dateValidation.reason})")
                if (!reasons.contains(ReviewReason.IMPOSSIBLE_DATE)) {
                    reasons.add(ReviewReason.IMPOSSIBLE_DATE)
                }
            }
        }

        // 4. URL Syntax & Dangerous Scheme Validation
        for (link in links) {
            val urlValidation = validateUrlSafety(link.url)
            if (!urlValidation.isValid) {
                issues.add("Unsafe or malformed URL: '${link.url}' (${urlValidation.reason})")
                if (urlValidation.isBlockedScheme && !reasons.contains(ReviewReason.LINK_UNSAFE)) {
                    reasons.add(ReviewReason.LINK_UNSAFE)
                } else if (!reasons.contains(ReviewReason.LINK_UNCERTAIN)) {
                    reasons.add(ReviewReason.LINK_UNCERTAIN)
                }
            }
        }

        // 5. Category-Specific Required Field Completeness
        val completenessIssue = validateCategoryCompleteness(category, extractedData, sourcePostDate, lastDate)
        if (completenessIssue != null) {
            issues.add(completenessIssue)
            if (!reasons.contains(ReviewReason.MISSING_CRITICAL_DATA)) {
                reasons.add(ReviewReason.MISSING_CRITICAL_DATA)
            }
        }

        // 6. Expiry Calculation
        var isExpired = false
        if (category == SmartContentCategory.VACANCY || category == SmartContentCategory.ADMISSION) {
            if (!lastDate.isNullOrBlank()) {
                if (SmartDateIntelligence.isValidDate(lastDate)) {
                    isExpired = SmartDateIntelligence.isDateExpired(lastDate)
                }
            } else {
                // Missing last date for vacancy/admission -> cannot be guaranteed active automatically
                reasons.add(ReviewReason.UNKNOWN_EXPIRY)
            }
        }

        val isValid = issues.isEmpty() && isCutoffEligible

        return ValidationResult(
            isValid = isValid,
            issues = issues,
            flaggedReasons = reasons,
            isExpired = isExpired,
            isCutoffEligible = isCutoffEligible
        )
    }

    /**
     * Validates calendar correctness including leap years and impossible day numbers (32 Aug, 31 Feb).
     */
    fun validateCalendarDate(dateStr: String): DateValidationResult {
        if (dateStr.isBlank()) return DateValidationResult(false, "Date string is blank")

        val clean = dateStr.trim()
        val isoParts = clean.split("-")
        if (isoParts.size == 3) {
            val year = isoParts[0].toIntOrNull() ?: return DateValidationResult(false, "Malformed year")
            val month = isoParts[1].toIntOrNull() ?: return DateValidationResult(false, "Malformed month")
            val day = isoParts[2].toIntOrNull() ?: return DateValidationResult(false, "Malformed day")

            if (year < 2000 || year > 2099) return DateValidationResult(false, "Year $year out of reasonable range")
            if (month < 1 || month > 12) return DateValidationResult(false, "Month $month is invalid")

            val maxDaysInMonth = when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
                else -> 0
            }

            if (day < 1 || day > maxDaysInMonth) {
                return DateValidationResult(false, "Day $day is impossible for month $month (max $maxDaysInMonth)")
            }

            return DateValidationResult(true, "Valid ISO date")
        }

        // Check fallback dd-MM-yyyy or dd/MM/yyyy
        val altParts = clean.split(Regex("[-/.]"))
        if (altParts.size == 3) {
            val day = altParts[0].toIntOrNull()
            val month = altParts[1].toIntOrNull()
            val year = altParts[2].toIntOrNull()

            if (day != null && month != null && year != null) {
                val fullYear = if (year < 100) 2000 + year else year
                if (month in 1..12) {
                    val maxDays = when (month) {
                        1, 3, 5, 7, 8, 10, 12 -> 31
                        4, 6, 9, 11 -> 30
                        2 -> if ((fullYear % 4 == 0 && fullYear % 100 != 0) || (fullYear % 400 == 0)) 29 else 28
                        else -> 0
                    }
                    if (day in 1..maxDays) {
                        return DateValidationResult(true, "Valid formatted date")
                    } else {
                        return DateValidationResult(false, "Day $day is impossible for month $month")
                    }
                }
            }
        }

        return DateValidationResult(false, "Unrecognized date format: $dateStr")
    }

    data class DateValidationResult(val isValid: Boolean, val reason: String)

    /**
     * Validates URL structure and ensures no malicious schemes are present.
     */
    fun validateUrlSafety(url: String): UrlValidationResult {
        if (url.isBlank()) return UrlValidationResult(false, "Empty URL", isBlockedScheme = false)
        val clean = url.trim()

        val lower = clean.lowercase()
        for (blocked in BLOCKED_SCHEMES) {
            if (lower.startsWith("$blocked:")) {
                return UrlValidationResult(false, "Blocked dangerous URL scheme: '$blocked'", isBlockedScheme = true)
            }
        }

        try {
            val uri = URI(clean)
            val scheme = uri.scheme?.lowercase()
            if (scheme == null) {
                return UrlValidationResult(false, "URL is missing scheme/protocol (http/https)", isBlockedScheme = false)
            }
            if (scheme !in ALLOWED_SCHEMES) {
                return UrlValidationResult(false, "Scheme '$scheme' is not allowed. Only HTTP/HTTPS permitted.", isBlockedScheme = true)
            }
            if (uri.host.isNullOrBlank()) {
                return UrlValidationResult(false, "URL is missing a valid host domain", isBlockedScheme = false)
            }
            return UrlValidationResult(true, "Valid safe web URL", isBlockedScheme = false, host = uri.host, isHttps = scheme == "https")
        } catch (e: Exception) {
            return UrlValidationResult(false, "Malformed URI: ${e.message}", isBlockedScheme = false)
        }
    }

    data class UrlValidationResult(
        val isValid: Boolean,
        val reason: String,
        val isBlockedScheme: Boolean,
        val host: String? = null,
        val isHttps: Boolean = false
    )

    /**
     * Ensures mandatory fields are populated for each category.
     */
    private fun validateCategoryCompleteness(
        category: SmartContentCategory,
        extractedData: SmartExtractedData,
        sourcePostDate: String?,
        lastDate: String?
    ): String? {
        val org = extractedData.organization
        val title = extractedData.title

        if (title.isBlank()) return "Missing title for category ${category.name}"

        return when (category) {
            SmartContentCategory.VACANCY -> {
                if (org.isNullOrBlank() || org.equals("Unknown", ignoreCase = true)) {
                    "Vacancy is missing organization name"
                } else null
            }
            SmartContentCategory.RESULT -> {
                if (org.isNullOrBlank()) "Result is missing organization/board name" else null
            }
            SmartContentCategory.ADMIT_CARD -> {
                if (org.isNullOrBlank()) "Admit card is missing organization/board name" else null
            }
            SmartContentCategory.ANSWER_KEY -> {
                if (org.isNullOrBlank()) "Answer key is missing organization/board name" else null
            }
            SmartContentCategory.ADMISSION -> {
                val inst = (extractedData as? SmartExtractedData.Admission)?.data?.institution ?: org
                if (inst.isNullOrBlank()) "Admission is missing institution/university name" else null
            }
            SmartContentCategory.OTHER -> null
        }
    }
}
