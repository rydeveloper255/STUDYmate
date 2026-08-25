package com.example.service.voice

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

/**
 * Text Normalization and Spoken Preprocessing Engine for NOVA Voice Synthesis.
 *
 * Cleans markdown formatting, removes code blocks and raw URLs, naturalizes numbers/dates/currencies,
 * preserves standard exam abbreviations (RRB, SSC, UPSC, IBPS), converts UI markers into natural speech,
 * and generates concise spoken summaries for long multi-paragraph AI explanations.
 */
object NovaVoiceTextNormalizer {

    // Common Indian Government & Recruitment exam acronyms to preserve
    private val EXAM_ACRONYMS = listOf(
        "RRB", "SSC", "UPSC", "IBPS", "NTA", "CTET", "NDA", "CDS",
        "NEET", "JEE", "GATE", "CAT", "State PSC", "UPPSC", "BPSC", "MPSC"
    )

    private val ISO_DATE_PATTERN = Pattern.compile("""(\d{4})-(\d{2})-(\d{2})(?:T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z?)?""")
    private val NUMERIC_DATE_PATTERN = Pattern.compile("""(\d{1,2})[-/](\d{1,2})[-/](\d{4})""")
    private val URL_PATTERN = Pattern.compile("""https?://\S+""")
    private val CODE_BLOCK_PATTERN = Pattern.compile("""```[\s\S]*?```""")
    private val INLINE_CODE_PATTERN = Pattern.compile("""`[^`]*`""")
    private val ACTION_TAG_PATTERN = Pattern.compile("""\[ACTION:.*?\]""", Pattern.CASE_INSENSITIVE)
    private val MEMORY_TAG_PATTERN = Pattern.compile("""\[MEMORY:.*?\]""", Pattern.CASE_INSENSITIVE)
    private val MARKDOWN_LINK_PATTERN = Pattern.compile("""\[([^\]]+)\]\([^\)]+\)""")

    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    /**
     * Primary entry point for cleaning and preparing text for ElevenLabs Text-to-Speech.
     *
     * @param rawText AI response, notification text, or explanation text.
     * @param maxSpokenLength Maximum character length for spoken output. If exceeded, a natural spoken summary is prepared.
     */
    fun normalizeForSpeech(rawText: String, maxSpokenLength: Int = 380): String {
        if (rawText.isBlank()) return ""

        var text = rawText.trim()

        // 1. Remove code blocks entirely as they are not suitable for speech
        text = CODE_BLOCK_PATTERN.matcher(text).replaceAll(" [Code snippet displayed on screen] ")
        text = INLINE_CODE_PATTERN.matcher(text).replaceAll(" ")

        // 2. Remove internal action and memory metadata tags
        text = ACTION_TAG_PATTERN.matcher(text).replaceAll("")
        text = MEMORY_TAG_PATTERN.matcher(text).replaceAll("")

        // 3. Conversational replacement for UI button patterns
        text = replaceUiActionTokens(text)

        // 4. Clean standard markdown links [Label](url) -> Label
        text = MARKDOWN_LINK_PATTERN.matcher(text).replaceAll("$1")

        // 5. Remove remaining raw URLs
        text = URL_PATTERN.matcher(text).replaceAll("")

        // 6. Naturalize ISO & numeric dates into conversational format (e.g. "18 September 2026")
        text = normalizeDates(text)

        // 7. Normalize currency and number representations
        text = normalizeCurrencyAndNumbers(text)

        // 8. Remove markdown formatting markers (*, **, #, _, ~, >, etc.)
        text = stripMarkdownStyling(text)

        // 9. Naturalize common mathematical / comparison symbols
        text = normalizeSymbols(text)

        // 10. Clean excessive emojis and whitespace
        text = cleanEmojisAndSpacing(text)

        // 11. Long text optimization: for long study notes or multi-paragraph responses,
        // create a concise spoken summary so user isn't stuck listening for 5 minutes.
        if (text.length > maxSpokenLength) {
            text = extractSpokenSummary(text, maxSpokenLength)
        }

        return text.trim()
    }

    /**
     * Converts UI action links to friendly spoken companion phrases.
     */
    private fun replaceUiActionTokens(text: String): String {
        var result = text
        result = result.replace(Regex("""\[View Vacancy\]""", RegexOption.IGNORE_CASE), " Main vacancy details open kar raha hoon. ")
        result = result.replace(Regex("""\[View Result\]""", RegexOption.IGNORE_CASE), " Result update check karte hain. ")
        result = result.replace(Regex("""\[View Admit Card\]""", RegexOption.IGNORE_CASE), " Admit card details screen par aa gayi hain. ")
        result = result.replace(Regex("""\[Start Quiz\]""", RegexOption.IGNORE_CASE), " Chalo practice quiz start karte hain. ")
        result = result.replace(Regex("""\[Apply\]""", RegexOption.IGNORE_CASE), " Official application portal open kar raha hoon. ")
        result = result.replace(Regex("""\[Explain More\]""", RegexOption.IGNORE_CASE), " Detail explanation dekh lete hain. ")
        result = result.replace(Regex("""\[Save Note\]""", RegexOption.IGNORE_CASE), " Smart note save kar liya hai. ")
        return result
    }

    /**
     * Converts numeric and ISO dates (e.g. 2026-09-18 or 18/09/2026) to "18 September 2026".
     */
    private fun normalizeDates(text: String): String {
        var result = text

        // Replace ISO 8601 timestamps
        val isoMatcher = ISO_DATE_PATTERN.matcher(result)
        val isoSb = StringBuffer()
        while (isoMatcher.find()) {
            try {
                val year = isoMatcher.group(1)
                val monthIdx = (isoMatcher.group(2)?.toIntOrNull() ?: 1) - 1
                val day = isoMatcher.group(3)?.toIntOrNull() ?: 1
                val monthName = MONTH_NAMES.getOrElse(monthIdx) { "Month" }
                val replacement = "$day $monthName $year"
                isoMatcher.appendReplacement(isoSb, replacement)
            } catch (e: Exception) {
                isoMatcher.appendReplacement(isoSb, isoMatcher.group(0))
            }
        }
        isoMatcher.appendTail(isoSb)
        result = isoSb.toString()

        // Replace DD/MM/YYYY or DD-MM-YYYY
        val dateMatcher = NUMERIC_DATE_PATTERN.matcher(result)
        val dateSb = StringBuffer()
        while (dateMatcher.find()) {
            try {
                val day = dateMatcher.group(1)?.toIntOrNull() ?: 1
                val monthIdx = (dateMatcher.group(2)?.toIntOrNull() ?: 1) - 1
                val year = dateMatcher.group(3) ?: ""
                if (monthIdx in 0..11 && day in 1..31) {
                    val monthName = MONTH_NAMES[monthIdx]
                    val replacement = if (year.isNotBlank()) "$day $monthName $year" else "$day $monthName"
                    dateMatcher.appendReplacement(dateSb, replacement)
                } else {
                    dateMatcher.appendReplacement(dateSb, dateMatcher.group(0))
                }
            } catch (e: Exception) {
                dateMatcher.appendReplacement(dateSb, dateMatcher.group(0))
            }
        }
        dateMatcher.appendTail(dateSb)

        return dateSb.toString()
    }

    /**
     * Converts currency symbols and number formats to spoken language.
     */
    private fun normalizeCurrencyAndNumbers(text: String): String {
        var result = text
        // Currency ₹500 -> 500 rupees
        result = result.replace(Regex("""₹\s*(\d+)"""), "$1 Rupees")
        result = result.replace(Regex("""Rs\.?\s*(\d+)"""), "$1 Rupees")
        // Percentages 95% -> 95 percent
        result = result.replace(Regex("""(\d+)%"""), "$1 percent")
        // Formatted thousands like 1,250 -> 1250
        result = result.replace(Regex("""(\d+),(\d{3})"""), "$1$2")
        return result
    }

    /**
     * Strips Markdown syntax characters.
     */
    private fun stripMarkdownStyling(text: String): String {
        return text
            .replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "") // Headers
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1") // Bold **
            .replace(Regex("""\*(.*?)\*"""), "$1") // Italic *
            .replace(Regex("""__(.*?)__"""), "$1") // Bold __
            .replace(Regex("""_(.*?)_"""), "$1") // Italic _
            .replace(Regex("""~~(.*?)~~"""), "$1") // Strikethrough
            .replace(Regex("""^[*-•]\s+""", RegexOption.MULTILINE), "") // Bullet points
            .replace(Regex("""^\d+\.\s+""", RegexOption.MULTILINE), "") // Numbered lists
            .replace(Regex("""^>\s+""", RegexOption.MULTILINE), "") // Quotes
            .replace("|", " ") // Table separators
    }

    /**
     * Conversational translation for comparison and logical symbols.
     */
    private fun normalizeSymbols(text: String): String {
        return text
            .replace("->", " leads to ")
            .replace("=>", " implies ")
            .replace("!=", " is not equal to ")
            .replace("<=", " less than or equal to ")
            .replace(">=", " greater than or equal to ")
            .replace("+/-", " plus or minus ")
            .replace("approx.", "approximately")
            .replace("approx", "approximately")
            .replace("mins", "minutes")
            .replace("min", "minute")
            .replace("hrs", "hours")
            .replace("hr", "hour")
            .replace("&", " and ")
            .replace("@", " at ")
    }

    /**
     * Strips decorative emojis and cleans up multiple spaces.
     */
    private fun cleanEmojisAndSpacing(text: String): String {
        // Remove common decorative emojis while preserving text characters
        val noEmoji = text.replace(Regex("[\\p{So}\\p{Cn}]"), "")
        return noEmoji.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Produces a concise, punchy spoken summary for long multi-paragraph texts.
     */
    private fun extractSpokenSummary(text: String, maxLen: Int): String {
        val sentences = text.split(Regex("""(?<=[.!?])\s+""")).filter { it.isNotBlank() }
        if (sentences.isEmpty()) return text.take(maxLen)

        val summarySb = StringBuilder()
        for (sentence in sentences) {
            if (summarySb.length + sentence.length <= maxLen) {
                if (summarySb.isNotEmpty()) summarySb.append(" ")
                summarySb.append(sentence)
            } else {
                break
            }
        }

        val result = summarySb.toString()
        return if (result.length >= 60) {
            result
        } else {
            text.take(maxLen).substringBeforeLast(" ") + "..."
        }
    }

    /**
     * Detects primary language of text ("HI" for Devanagari Hindi, "HINGLISH", or "EN" for English).
     */
    fun detectLanguage(text: String): String {
        val devanagariCount = text.count { it in '\u0900'..'\u097F' }
        if (devanagariCount > 10) return "HI"

        val lower = text.lowercase()
        val hinglishKeywords = listOf(
            "bhai", "aaj", "ka", "ki", "ke", "hai", "hain", "karo", "karein", "chalo",
            "tumhara", "tumhari", "mujhe", "kyu", "kya", "nahi", "aaya", "mil", "gaya"
        )
        val containsHinglish = hinglishKeywords.count { lower.contains(" $it ") || lower.startsWith("$it ") || lower.endsWith(" $it") }
        if (containsHinglish >= 2) return "HINGLISH"

        return "EN"
    }
}
