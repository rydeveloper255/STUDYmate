package com.example.service.intelligence.smart

import java.net.URI
import java.util.regex.Pattern

/**
 * Step 83: Content Cleaner.
 * 
 * Performs non-destructive text sanitization:
 * - Collapses excessive whitespace, tabs, and excessive blank lines (max 2 consecutive newlines).
 * - Strips promotional spam / social media boilerplate (e.g. "Join Telegram", "Forward this message").
 * - Sanitizes URLs by safely stripping tracking query parameters (utm_*, fbclid, gclid, etc.)
 *   without altering critical routing parameters.
 * - Deduplicates redundant URLs within the text.
 * - Strictly preserves the original semantic facts.
 */
object SmartContentCleaner {

    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "igshid", "ref", "ref_src", "source"
    )

    private val PROMOTIONAL_PATTERNS = listOf(
        Pattern.compile("(?i)(?:join|follow|subscribe)\\s+(?:our\\s+)?(?:telegram|whatsapp|youtube|instagram|channel|group)[^\\n]*"),
        Pattern.compile("(?i)(?:share|forward)\\s+(?:this|with)\\s+(?:post|message|friends|groups)[^\\n]*"),
        Pattern.compile("(?i)रोजगार\\s+समाचार\\s+के\\s+लिए\\s+ग्रुप\\s+से\\s+जुड़ें[^\\n]*"),
        Pattern.compile("(?i)अपने\\s+दोस्तों\\s+को\\s+भी\\s+शेयर\\s+करें[^\\n]*"),
        Pattern.compile("(?i)क्लिक\\s+करें\\s+और\\s+जुड़ें[^\\n]*")
    )

    private val URL_PATTERN = Pattern.compile(
        "https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Cleans raw incoming content string.
     */
    fun cleanContent(rawText: String): String {
        if (rawText.isBlank()) return ""

        var cleaned = rawText

        // 1. Remove promotional patterns
        for (pattern in PROMOTIONAL_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll("")
        }

        // 2. Clean URLs in text (strip tracking parameters and deduplicate)
        cleaned = cleanUrlsInText(cleaned)

        // 3. Normalize whitespace & line breaks
        // Replace multiple horizontal spaces/tabs with a single space
        cleaned = cleaned.replace(Regex("[ \\t]+"), " ")

        // Replace 3 or more newlines with double newline
        cleaned = cleaned.replace(Regex("\\n{3,}"), "\n\n")

        // Trim each line and eliminate trailing blank lines
        val lines = cleaned.lines().map { it.trim() }
        cleaned = lines.joinToString("\n").trim()

        return cleaned
    }

    /**
     * Sanitizes a single URL by removing known tracking parameters.
     */
    fun sanitizeUrl(url: String): String {
        return try {
            val trimmed = url.trim().trimEnd('.', ',', ')', ']', ';', '>', '<')
            if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                return trimmed
            }
            val uri = URI(trimmed)
            val query = uri.query
            if (query.isNullOrBlank()) {
                return trimmed
            }

            val queryParams = query.split("&")
            val filteredParams = queryParams.filter { param ->
                val key = param.substringBefore("=").lowercase()
                !TRACKING_PARAMS.contains(key)
            }

            val newQuery = if (filteredParams.isNotEmpty()) "?${filteredParams.joinToString("&")}" else ""
            val portStr = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            val fragmentStr = if (!uri.fragment.isNullOrBlank()) "#${uri.fragment}" else ""
            "${uri.scheme}://${uri.host}$portStr${uri.rawPath ?: ""}$newQuery$fragmentStr"
        } catch (e: Exception) {
            url.trim().trimEnd('.', ',', ')', ']', ';')
        }
    }

    private fun cleanUrlsInText(text: String): String {
        val matcher = URL_PATTERN.matcher(text)
        val seenUrls = mutableSetOf<String>()
        val sb = StringBuffer()

        while (matcher.find()) {
            val originalUrl = matcher.group()
            val sanitized = sanitizeUrl(originalUrl)

            if (seenUrls.contains(sanitized)) {
                // Remove duplicate URL mention in the body if already present
                matcher.appendReplacement(sb, "")
            } else {
                seenUrls.add(sanitized)
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(sanitized))
            }
        }
        matcher.appendTail(sb)
        return sb.toString()
    }
}
