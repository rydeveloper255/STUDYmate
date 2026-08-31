package com.example.service.intelligence.verification

import com.example.service.intelligence.smart.SmartCategoryClassifier
import com.example.service.intelligence.smart.SmartContentCategory
import com.example.service.intelligence.smart.SmartExtractedData
import com.example.service.intelligence.smart.SmartTitleNormalizer
import java.util.regex.Pattern

/**
 * Step 84: Layer 2 — Intelligent & Contextual Validation Engine.
 * 
 * Performs semantic, contextual, and cross-field validation:
 * - Category Conflict Verification (detects category misassignment against content keywords)
 * - Vacancy Verification (verifies job/recruitment presence and details)
 * - Chronological & Logical Consistency (Start Date vs Last Date vs Exam Date)
 * - AI Hallucination Protection & Source Text Traceability (zero invented facts)
 * - Organization Verification (no guessed organizations)
 * - Structured Vacancy Count Validation
 */
object IntelligentValidator {

    private val RESULT_STRONG_SIGNALS = listOf(
        "result declared", "merit list", "final result", "cutoff marks", "cut-off",
        "scorecard released", "marksheet", "परिणाम जारी", "रिजल्ट घोषित"
    )

    private val ADMIT_CARD_STRONG_SIGNALS = listOf(
        "admit card", "hall ticket", "call letter", "city slip", "city intimation",
        "प्रवेश पत्र", "एडमिट कार्ड जारी"
    )

    private val ANSWER_KEY_STRONG_SIGNALS = listOf(
        "answer key", "response sheet", "objection tracker", "tentative key",
        "उत्तर कुंजी", "आंसर की"
    )

    private val ADMISSION_STRONG_SIGNALS = listOf(
        "admission form", "counseling round", "seat allotment", "ug admission", "pg admission",
        "प्रवेश सूचना", "काउंसलिंग"
    )

    private val VACANCY_STRONG_SIGNALS = listOf(
        "recruitment", "vacancy", "bharti", "posts", "invited application", "apply online",
        "भर्ती", "पदों पर भर्ती", "ऑनलाइन आवेदन"
    )

    data class IntelligentValidationResult(
        val passed: Boolean,
        val issues: List<String>,
        val flaggedReasons: List<ReviewReason>,
        val sanitizedExtractedData: SmartExtractedData,
        val sanitizedLastDate: String?,
        val sanitizedExamDate: String?
    )

    /**
     * Executes intelligent semantic verification on extracted content.
     */
    fun validate(
        rawText: String,
        cleanedText: String,
        category: SmartContentCategory,
        extractedData: SmartExtractedData,
        sourcePostDate: String?,
        lastDate: String?,
        examDate: String?
    ): IntelligentValidationResult {
        val issues = mutableListOf<String>()
        val reasons = mutableListOf<ReviewReason>()
        val combinedText = "$rawText\n$cleanedText\n${extractedData.title}".lowercase()

        // 1. Category Conflict Verification
        val categoryConflict = detectCategoryConflict(category, combinedText)
        if (categoryConflict != null) {
            issues.add(categoryConflict)
            reasons.add(ReviewReason.CATEGORY_CONFLICT)
        }

        // 2. Chronological & Date Consistency Check
        val startDate = when (extractedData) {
            is SmartExtractedData.Vacancy -> extractedData.data.applicationStartDate
            is SmartExtractedData.Admission -> extractedData.data.applicationStartDate
            else -> sourcePostDate
        }

        val dateConflict = checkDateChronology(startDate, lastDate, examDate)
        if (dateConflict != null) {
            issues.add(dateConflict)
            reasons.add(ReviewReason.DATE_CONFLICT)
        }

        // 3. AI Hallucination & Source Traceability Verification
        var verifiedLastDate = lastDate
        var verifiedExamDate = examDate

        if (!lastDate.isNullOrBlank()) {
            val isDateInSource = isDateTraceableInSource(lastDate, rawText)
            if (!isDateInSource) {
                issues.add("Extracted last date '$lastDate' is NOT present in source text (AI hallucination protection triggered)")
                reasons.add(ReviewReason.POTENTIAL_HALLUCINATION)
                verifiedLastDate = null // Nullify hallucinated date
            }
        }

        if (!examDate.isNullOrBlank()) {
            val isDateInSource = isDateTraceableInSource(examDate, rawText)
            if (!isDateInSource) {
                issues.add("Extracted exam date '$examDate' is NOT present in source text (AI hallucination protection triggered)")
                reasons.add(ReviewReason.POTENTIAL_HALLUCINATION)
                verifiedExamDate = null // Nullify hallucinated date
            }
        }

        // 4. Organization Validation
        val org = extractedData.organization
        if (!org.isNullOrBlank() && !isOrganizationTraceableInSource(org, rawText)) {
            issues.add("Extracted organization '$org' is not clearly supported by source text")
            reasons.add(ReviewReason.ORGANIZATION_UNCERTAIN)
        }

        // 5. Vacancy Specific Verification
        if (category == SmartContentCategory.VACANCY) {
            val isVacancyEstablished = VACANCY_STRONG_SIGNALS.any { combinedText.contains(it) }
            if (!isVacancyEstablished && (extractedData as? SmartExtractedData.Vacancy)?.data?.vacancyCount == null) {
                issues.add("Content does not clearly establish an active recruitment/vacancy")
                if (!reasons.contains(ReviewReason.LOW_CONFIDENCE)) {
                    reasons.add(ReviewReason.LOW_CONFIDENCE)
                }
            }
        }

        // Build sanitized extracted data with verified dates
        val sanitizedExtractedData = when (extractedData) {
            is SmartExtractedData.Vacancy -> {
                val data = extractedData.data.copy(
                    lastDate = verifiedLastDate,
                    examDate = verifiedExamDate
                )
                SmartExtractedData.Vacancy(data)
            }
            is SmartExtractedData.Admission -> {
                val data = extractedData.data.copy(
                    lastDate = verifiedLastDate
                )
                SmartExtractedData.Admission(data)
            }
            is SmartExtractedData.Result -> {
                val data = extractedData.data.copy(
                    examDate = verifiedExamDate
                )
                SmartExtractedData.Result(data)
            }
            is SmartExtractedData.AdmitCard -> {
                val data = extractedData.data.copy(
                    examDate = verifiedExamDate
                )
                SmartExtractedData.AdmitCard(data)
            }
            is SmartExtractedData.AnswerKey -> {
                val data = extractedData.data.copy(
                    examDate = verifiedExamDate
                )
                SmartExtractedData.AnswerKey(data)
            }
            is SmartExtractedData.Other -> extractedData
        }

        val passed = reasons.isEmpty()

        return IntelligentValidationResult(
            passed = passed,
            issues = issues,
            flaggedReasons = reasons,
            sanitizedExtractedData = sanitizedExtractedData,
            sanitizedLastDate = verifiedLastDate,
            sanitizedExamDate = verifiedExamDate
        )
    }

    /**
     * Cross-verifies category against conflicting context signals.
     */
    private fun detectCategoryConflict(category: SmartContentCategory, text: String): String? {
        when (category) {
            SmartContentCategory.VACANCY -> {
                if (RESULT_STRONG_SIGNALS.any { text.contains(it) } && !text.contains("new vacancy") && !text.contains("apply online")) {
                    return "Category conflict: Content clearly indicates Result/Merit List, but assigned category is VACANCY"
                }
                if (ADMIT_CARD_STRONG_SIGNALS.any { text.contains(it) } && !text.contains("apply online")) {
                    return "Category conflict: Content clearly indicates Admit Card, but assigned category is VACANCY"
                }
                if (ANSWER_KEY_STRONG_SIGNALS.any { text.contains(it) }) {
                    return "Category conflict: Content clearly indicates Answer Key, but assigned category is VACANCY"
                }
            }
            SmartContentCategory.RESULT -> {
                if (ADMIT_CARD_STRONG_SIGNALS.any { text.contains(it) } && !text.contains("result")) {
                    return "Category conflict: Content indicates Admit Card, but assigned category is RESULT"
                }
                if (ANSWER_KEY_STRONG_SIGNALS.any { text.contains(it) } && !text.contains("result")) {
                    return "Category conflict: Content indicates Answer Key, but assigned category is RESULT"
                }
            }
            SmartContentCategory.ADMIT_CARD -> {
                if (RESULT_STRONG_SIGNALS.any { text.contains(it) } && !text.contains("admit card")) {
                    return "Category conflict: Content indicates Result, but assigned category is ADMIT_CARD"
                }
            }
            SmartContentCategory.ANSWER_KEY -> {
                if (RESULT_STRONG_SIGNALS.any { text.contains(it) } && !text.contains("answer key")) {
                    return "Category conflict: Content indicates Result, but assigned category is ANSWER_KEY"
                }
            }
            SmartContentCategory.ADMISSION -> {
                if (RESULT_STRONG_SIGNALS.any { text.contains(it) } && !text.contains("admission")) {
                    return "Category conflict: Content indicates Result, but assigned category is ADMISSION"
                }
            }
            SmartContentCategory.OTHER -> null
        }
        return null
    }

    /**
     * Verifies chronological order of start date, last date, and exam date.
     */
    private fun checkDateChronology(startDate: String?, lastDate: String?, examDate: String?): String? {
        if (!startDate.isNullOrBlank() && !lastDate.isNullOrBlank()) {
            if (startDate > lastDate) {
                return "Date Conflict: Application start date '$startDate' is after last date '$lastDate'"
            }
        }
        if (!startDate.isNullOrBlank() && !examDate.isNullOrBlank()) {
            if (startDate > examDate) {
                return "Date Conflict: Application start date '$startDate' is after exam date '$examDate'"
            }
        }
        return null
    }

    /**
     * Checks if a date string or its components (day, month, year) actually exist in the source text.
     */
    fun isDateTraceableInSource(dateStr: String, sourceText: String): Boolean {
        if (sourceText.isBlank()) return false
        val clean = dateStr.trim()
        if (sourceText.contains(clean, ignoreCase = true)) return true

        // Check if day and month/year appear in source
        val parts = clean.split(Regex("[-/.]"))
        if (parts.size == 3) {
            val (p1, p2, p3) = parts
            val day = if (p1.length <= 2) p1 else p3
            val year = if (p1.length == 4) p1 else p3

            val dayInt = day.toIntOrNull() ?: return false
            val dayPadded = String.format("%02d", dayInt)
            val dayUnpadded = dayInt.toString()

            val hasDay = sourceText.contains(dayPadded) || sourceText.contains(dayUnpadded)
            val hasYear = sourceText.contains(year)

            if (hasDay && (hasYear || sourceText.contains("2026"))) {
                return true
            }
        }
        return false
    }

    /**
     * Verifies that the organization is not completely made up.
     */
    fun isOrganizationTraceableInSource(org: String, sourceText: String): Boolean {
        if (sourceText.isBlank() || org.isBlank()) return false
        val cleanOrg = org.trim().lowercase()
        val cleanText = sourceText.lowercase()

        if (cleanText.contains(cleanOrg)) return true

        val words = cleanOrg.split(" ").filter { it.length > 1 }
        if (words.isEmpty()) return true

        val acronym = words.mapNotNull { it.firstOrNull() }.joinToString("")
        if (acronym.length >= 2 && cleanText.contains(acronym)) {
            return true
        }

        val significantWords = words.filter { it.length > 2 }
        if (significantWords.isEmpty()) return true
        val matchedWords = significantWords.count { cleanText.contains(it) }
        return (matchedWords.toFloat() / significantWords.size) >= 0.5f
    }

    /**
     * Extracts and validates structured vacancy counts from source text.
     */
    fun validateVacancyCount(sourceText: String, claimedCount: Int?): Int? {
        if (claimedCount == null) return null
        if (sourceText.contains(claimedCount.toString())) {
            return claimedCount
        }
        return null
    }
}
