package com.example.service.content.whatsapp

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.regex.Pattern

/**
 * Step 82: Structured Content Extractor for WhatsApp Channel posts.
 * Extracts title, organization, dates, vacancy counts, links, and attachments without creating fake data.
 */
object WhatsAppContentExtractor {

    private val URL_PATTERN = Pattern.compile(
        "https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    private const val DATE_REGEX = "(?:[0-9]{4}[-/.][0-9]{1,2}[-/.][0-9]{1,2}|[0-9]{1,2}[-/.][0-9]{1,2}[-/.][0-9]{2,4}|[0-9]{1,2}\\s+[A-Za-z]+\\s+[0-9]{4})"

    private val LAST_DATE_PATTERNS = listOf(
        Pattern.compile("(?:last\\s*date|apply\\s*(?:online\\s*)?till|apply\\s*till|closing\\s*date|deadline|अंतिम\\s*तिथि|आवेदन\\s*की\\s*अंतिम\\s*तिथि)[:\\s]*($DATE_REGEX)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:extended\\s*to|बढ़ाकर|last\\s*date\\s*extended)[:\\s]*($DATE_REGEX)", Pattern.CASE_INSENSITIVE)
    )

    private val EXAM_DATE_PATTERNS = listOf(
        Pattern.compile("(?:exam\\s*date|cbt\\s*date|परीक्षा\\s*तिथि|परीक्षा\\s*दिनांक)[:\\s]*($DATE_REGEX|[A-Za-z]+\\s+[0-9]{4})", Pattern.CASE_INSENSITIVE)
    )

    private val POST_DATE_PATTERNS = listOf(
        Pattern.compile("(?:post\\s*date|start\\s*date|published\\s*date|जारी\\s*तिथि|आवेदन\\s*शुरू)[:\\s]*($DATE_REGEX)", Pattern.CASE_INSENSITIVE)
    )

    private val TOTAL_VACANCY_PATTERNS = listOf(
        Pattern.compile("(?:total\\s*vacanc(?:y|ies)|total\\s*posts?|कुल\\s*पद|पदों\\s*की\\s*संख्या)[:\\s]*([0-9,]+)", Pattern.CASE_INSENSITIVE)
    )

    private val KNOWN_ORGS = listOf(
        "UPSC" to "Union Public Service Commission (UPSC)",
        "SSC" to "Staff Selection Commission (SSC)",
        "RRB" to "Railway Recruitment Boards (RRB)",
        "RRC" to "Railway Recruitment Cell (RRC)",
        "IBPS" to "Institute of Banking Personnel Selection (IBPS)",
        "SBI" to "State Bank of India (SBI)",
        "BPSC" to "Bihar Public Service Commission (BPSC)",
        "UPSSSC" to "Uttar Pradesh Subordinate Services Selection Commission (UPSSSC)",
        "UPPSC" to "Uttar Pradesh Public Service Commission (UPPSC)",
        "DSSSB" to "Delhi Subordinate Services Selection Board (DSSSB)",
        "NTA" to "National Testing Agency (NTA)",
        "CBSE" to "Central Board of Secondary Education (CBSE)",
        "IGNOU" to "Indira Gandhi National Open University (IGNOU)",
        "DU" to "Delhi University (DU)",
        "IIT" to "Indian Institute of Technology (IIT)",
        "ISRO" to "Indian Space Research Organisation (ISRO)",
        "DRDO" to "Defence Research and Development Organisation (DRDO)",
        "CISF" to "Central Industrial Security Force (CISF)",
        "CRPF" to "Central Reserve Police Force (CRPF)",
        "BSF" to "Border Security Force (BSF)",
        "ITBP" to "Indo-Tibetan Border Police (ITBP)",
        "SSB" to "Sashastra Seema Bal (SSB)",
        "High Court" to "High Court of Judicature",
        "Police" to "State Police Recruitment Board"
    )

    /**
     * Extracts structured content from raw post.
     */
    fun extractContent(rawPost: WhatsAppRawPost): WhatsAppProcessedContent {
        val lines = rawPost.rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        
        // 1. Title Extraction
        val title = extractTitle(lines, rawPost.rawText)

        // 2. Category Detection
        val category = WhatsAppCategoryDetector.detectCategory(title, rawPost.rawText)

        // 3. Organization Extraction
        val organization = extractOrganization(title, rawPost.rawText)

        // 4. Dates Extraction
        val extractedPostDate = extractDate(rawPost.rawText, POST_DATE_PATTERNS) ?: rawPost.sourcePostDate
        val canonicalPostDate = extractedPostDate?.let { normalizeDate(it) } ?: rawPost.sourcePostDate
        val rawLastDate = extractDate(rawPost.rawText, LAST_DATE_PATTERNS)
        val canonicalLastDate = rawLastDate?.let { normalizeDate(it) }
        val rawExamDate = extractDate(rawPost.rawText, EXAM_DATE_PATTERNS)
        val canonicalExamDate = rawExamDate?.let { normalizeDate(it) ?: it }

        // 5. Total Vacancies
        val totalVacancies = extractTotalVacancies(rawPost.rawText)

        // 6. Links & Classification
        val extractedLinks = extractAndClassifyLinks(rawPost.rawText, category)

        val officialUrl = extractedLinks.firstOrNull { it.linkType == ExtractedLinkType.OFFICIAL_WEBSITE }?.url
        val applyUrl = extractedLinks.firstOrNull { it.linkType == ExtractedLinkType.APPLY_LINK }?.url
        val pdfUrl = extractedLinks.firstOrNull { it.linkType == ExtractedLinkType.PDF_LINK }?.url
        val resultUrl = extractedLinks.firstOrNull { it.linkType == ExtractedLinkType.RESULT_LINK }?.url

        // 7. Attachments
        val attachments = mutableListOf<ExtractedAttachment>()
        if (pdfUrl != null) {
            attachments.add(
                ExtractedAttachment(
                    attachmentType = "PDF",
                    reference = "Notification PDF",
                    url = pdfUrl
                )
            )
        }
        if (!rawPost.attachmentReference.isNullOrBlank()) {
            attachments.add(
                ExtractedAttachment(
                    attachmentType = if (rawPost.attachmentReference.endsWith(".pdf", ignoreCase = true)) "PDF" else "DOCUMENT",
                    reference = rawPost.attachmentReference,
                    url = pdfUrl
                )
            )
        }

        // 8. Deduplication Hash
        val contentHash = computeContentHash(
            title = title,
            category = category.key,
            postDate = canonicalPostDate,
            officialUrl = officialUrl ?: applyUrl ?: pdfUrl
        )

        // 9. Clean Description
        val description = buildCleanDescription(lines, title)

        return WhatsAppProcessedContent(
            id = rawPost.sourceMessageId?.let { "wa_$it" } ?: UUID.randomUUID().toString(),
            sourceMessageId = rawPost.sourceMessageId,
            title = title,
            category = category,
            description = description,
            sourceUrl = rawPost.sourceUrl,
            officialUrl = officialUrl,
            applyUrl = applyUrl,
            pdfUrl = pdfUrl,
            resultUrl = resultUrl,
            postDate = canonicalPostDate,
            lastDate = canonicalLastDate,
            examDate = canonicalExamDate,
            organization = organization,
            postName = extractPostName(title, organization),
            totalVacancies = totalVacancies,
            sourceType = WhatsAppSourceConfig.SOURCE_TYPE,
            sourceName = WhatsAppSourceConfig.SOURCE_NAME,
            status = WhatsAppIngestionStatus.PROCESSING,
            links = extractedLinks,
            attachments = attachments,
            contentHash = contentHash,
            fetchedAt = rawPost.fetchedAt,
            createdAt = rawPost.sourceTimestamp ?: rawPost.fetchedAt,
            updatedAt = System.currentTimeMillis(),
            isActive = true
        )
    }

    private fun extractTitle(lines: List<String>, fullText: String): String {
        if (lines.isEmpty()) return "Official Update"
        
        // Skip leading tags or emoji headers
        for (line in lines.take(3)) {
            val cleaned = cleanFormatting(line)
            if (cleaned.length >= 8) {
                return cleaned
            }
        }
        return cleanFormatting(lines.first())
    }

    private fun extractOrganization(title: String, fullText: String): String? {
        val combined = "$title $fullText"
        for ((shortName, fullName) in KNOWN_ORGS) {
            val p = Pattern.compile("\\b$shortName\\b", Pattern.CASE_INSENSITIVE)
            if (p.matcher(combined).find()) {
                return fullName
            }
        }
        return null
    }

    private fun extractPostName(title: String, organization: String?): String {
        var cleanTitle = title
        if (!organization.isNullOrBlank()) {
            cleanTitle = cleanTitle.replace(organization, "", ignoreCase = true)
        }
        cleanTitle = cleanTitle.replace("Recruitment 2026", "", ignoreCase = true)
            .replace("Recruitment 2027", "", ignoreCase = true)
            .replace("Notification", "", ignoreCase = true)
            .replace("Notification 2026", "", ignoreCase = true)
            .trim()
        return cleanTitle.ifBlank { title }
    }

    private fun extractDate(text: String, patterns: List<Pattern>): String? {
        for (pattern in patterns) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val match = m.group(1)?.trim()
                if (!match.isNullOrBlank()) {
                    return match
                }
            }
        }
        return null
    }

    private fun extractTotalVacancies(text: String): Int? {
        for (pattern in TOTAL_VACANCY_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val digits = m.group(1)?.replace(",", "")?.trim()
                val parsed = digits?.toIntOrNull()
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }
        return null
    }

    fun extractAndClassifyLinks(text: String, category: WhatsAppCategory): List<ExtractedLink> {
        val matcher = URL_PATTERN.matcher(text)
        val links = mutableListOf<ExtractedLink>()
        val seen = mutableSetOf<String>()

        while (matcher.find()) {
            val url = matcher.group().trim().trimEnd('.', ',', ')', ']', ';')
            if (url.isBlank() || seen.contains(url)) continue
            seen.add(url)

            val lower = url.lowercase()
            val linkType = when {
                lower.endsWith(".pdf") || lower.contains("notices") || lower.contains("advt") || lower.contains("notification") -> ExtractedLinkType.PDF_LINK
                lower.contains("apply") || lower.contains("registration") || lower.contains("login") || lower.contains("online") -> ExtractedLinkType.APPLY_LINK
                lower.contains("result") || lower.contains("score") || lower.contains("marks") -> ExtractedLinkType.RESULT_LINK
                lower.contains("answer") || lower.contains("key") || lower.contains("objection") -> ExtractedLinkType.ANSWER_KEY_LINK
                lower.contains(".gov.in") || lower.contains(".nic.in") || lower.contains(".ac.in") || lower.contains(".edu.in") -> ExtractedLinkType.OFFICIAL_WEBSITE
                else -> ExtractedLinkType.OTHER_LINK
            }

            links.add(
                ExtractedLink(
                    url = url,
                    linkType = linkType,
                    displayText = linkType.label
                )
            )
        }
        return links
    }

    private fun buildCleanDescription(lines: List<String>, title: String): String {
        val filtered = lines.filter { line ->
            val clean = cleanFormatting(line)
            clean != title && clean.isNotBlank()
        }
        return if (filtered.isNotEmpty()) {
            filtered.joinToString("\n")
        } else {
            title
        }
    }

    private fun cleanFormatting(text: String): String {
        return text.replace(Regex("[*#_~`]+"), "")
            .replace(Regex("^[•\\-–—]+\\s*"), "")
            .trim()
    }

    private fun normalizeDate(raw: String): String? {
        val parsed = WhatsAppSourceConfig.parseDateSafely(raw) ?: return null
        return WhatsAppSourceConfig.formatDateToIso(parsed)
    }

    /**
     * Deduplication hash computation
     */
    fun computeContentHash(
        title: String,
        category: String,
        postDate: String?,
        officialUrl: String?
    ): String {
        val normTitle = title.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val normCat = category.trim().lowercase()
        val normDate = postDate?.trim() ?: ""
        val normUrl = officialUrl?.trim()?.lowercase() ?: ""

        val raw = "$normTitle|$normCat|$normDate|$normUrl"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
