package com.example.service.intelligence.smart

import java.net.URI
import java.util.regex.Pattern

/**
 * Step 83: Smart Link Intelligence.
 * 
 * Extracts, sanitizes, and contextually classifies all links in incoming posts:
 * - Differentiates Official Website, Apply Link, Result Link, Admit Card Link, Answer Key Link, Admission Link, and PDF.
 * - Robust PDF detection without hallucinating PDF types.
 * - Safely handles malformed / invalid URLs without crashing.
 */
object SmartLinkClassifier {

    private val URL_PATTERN = Pattern.compile(
        "https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    private val OFFICIAL_DOMAINS = setOf(
        ".gov.in", ".nic.in", ".ac.in", ".edu.in", ".org.in", ".res.in"
    )

    /**
     * Extracts and contextually classifies links found in the text.
     */
    fun extractAndClassifyLinks(text: String, category: SmartContentCategory): List<SmartLink> {
        val matcher = URL_PATTERN.matcher(text)
        val links = mutableListOf<SmartLink>()
        val seenUrls = mutableSetOf<String>()

        while (matcher.find()) {
            val rawUrl = matcher.group()
            val sanitized = SmartContentCleaner.sanitizeUrl(rawUrl)
            if (sanitized.isBlank() || seenUrls.contains(sanitized)) continue
            seenUrls.add(sanitized)

            val lowerUrl = sanitized.lowercase()
            val isPdf = isActualPdfUrl(lowerUrl, text, sanitized)
            
            val linkType = when {
                isPdf -> SmartLinkType.PDF
                isApplyUrl(lowerUrl, text) -> SmartLinkType.APPLY
                isResultUrl(lowerUrl, text) -> SmartLinkType.RESULT
                isAdmitCardUrl(lowerUrl, text) -> SmartLinkType.ADMIT_CARD
                isAnswerKeyUrl(lowerUrl, text) -> SmartLinkType.ANSWER_KEY
                isAdmissionUrl(lowerUrl, text) -> SmartLinkType.ADMISSION
                isOfficialDomain(lowerUrl) -> SmartLinkType.OFFICIAL
                else -> {
                    // Fall back to category-aligned default if strongly suggested by context
                    when (category) {
                        SmartContentCategory.VACANCY -> if (lowerUrl.contains("recruitment") || lowerUrl.contains("career")) SmartLinkType.APPLY else SmartLinkType.OTHER
                        SmartContentCategory.RESULT -> if (lowerUrl.contains("score") || lowerUrl.contains("marks")) SmartLinkType.RESULT else SmartLinkType.OTHER
                        SmartContentCategory.ADMIT_CARD -> if (lowerUrl.contains("hall") || lowerUrl.contains("ticket")) SmartLinkType.ADMIT_CARD else SmartLinkType.OTHER
                        SmartContentCategory.ANSWER_KEY -> if (lowerUrl.contains("sheet") || lowerUrl.contains("key")) SmartLinkType.ANSWER_KEY else SmartLinkType.OTHER
                        SmartContentCategory.ADMISSION -> if (lowerUrl.contains("admission") || lowerUrl.contains("counsel")) SmartLinkType.ADMISSION else SmartLinkType.OTHER
                        else -> SmartLinkType.OTHER
                    }
                }
            }

            links.add(
                SmartLink(
                    url = sanitized,
                    linkType = linkType,
                    displayText = linkType.label,
                    isVerifiedPdf = isPdf
                )
            )
        }

        return links
    }

    /**
     * Checks if a URL is an actual PDF document without guessing.
     */
    fun isActualPdfUrl(lowerUrl: String, fullText: String, originalUrl: String): Boolean {
        if (lowerUrl.endsWith(".pdf") || lowerUrl.contains(".pdf?") || lowerUrl.contains("/pdf/")) {
            return true
        }

        // Check if context preceding the URL explicitly says "PDF" or "Download Notification PDF"
        val precedingSnippet = getPrecedingText(fullText, originalUrl)
        if (precedingSnippet.contains("pdf", ignoreCase = true) && 
            (lowerUrl.contains("notices") || lowerUrl.contains("advt") || lowerUrl.contains("notification") || lowerUrl.contains("circular"))) {
            return true
        }

        return false
    }

    private fun isApplyUrl(lowerUrl: String, fullText: String): Boolean {
        if (lowerUrl.contains("apply") || lowerUrl.contains("registration") || lowerUrl.contains("onlineform") || lowerUrl.contains("/app/")) {
            return true
        }
        val preceding = getPrecedingText(fullText, lowerUrl)
        return preceding.contains("apply online", ignoreCase = true) || preceding.contains("आवेदन लिंक", ignoreCase = true)
    }

    private fun isResultUrl(lowerUrl: String, fullText: String): Boolean {
        if (lowerUrl.contains("result") || lowerUrl.contains("scorecard") || lowerUrl.contains("marksheet") || lowerUrl.contains("meritlist")) {
            return true
        }
        val preceding = getPrecedingText(fullText, lowerUrl)
        return preceding.contains("result link", ignoreCase = true) || preceding.contains("रिजल्ट लिंक", ignoreCase = true)
    }

    private fun isAdmitCardUrl(lowerUrl: String, fullText: String): Boolean {
        if (lowerUrl.contains("admit") || lowerUrl.contains("hallticket") || lowerUrl.contains("callletter") || lowerUrl.contains("cityslip")) {
            return true
        }
        val preceding = getPrecedingText(fullText, lowerUrl)
        return preceding.contains("admit card", ignoreCase = true) || preceding.contains("एडमिट कार्ड", ignoreCase = true)
    }

    private fun isAnswerKeyUrl(lowerUrl: String, fullText: String): Boolean {
        if (lowerUrl.contains("answerkey") || lowerUrl.contains("answer-key") || lowerUrl.contains("objection") || lowerUrl.contains("responsesheet")) {
            return true
        }
        val preceding = getPrecedingText(fullText, lowerUrl)
        return preceding.contains("answer key", ignoreCase = true) || preceding.contains("उत्तर कुंजी", ignoreCase = true)
    }

    private fun isAdmissionUrl(lowerUrl: String, fullText: String): Boolean {
        if (lowerUrl.contains("admission") || lowerUrl.contains("counseling") || lowerUrl.contains("allotment") || lowerUrl.contains("seat")) {
            return true
        }
        val preceding = getPrecedingText(fullText, lowerUrl)
        return preceding.contains("admission", ignoreCase = true) || preceding.contains("काउंसलिंग", ignoreCase = true)
    }

    private fun isOfficialDomain(lowerUrl: String): Boolean {
        return try {
            val uri = URI(lowerUrl)
            val host = uri.host?.lowercase() ?: return false
            OFFICIAL_DOMAINS.any { host.endsWith(it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun getPrecedingText(fullText: String, url: String): String {
        val idx = fullText.indexOf(url)
        if (idx <= 0) return ""
        val start = (idx - 60).coerceAtLeast(0)
        return fullText.substring(start, idx)
    }
}
