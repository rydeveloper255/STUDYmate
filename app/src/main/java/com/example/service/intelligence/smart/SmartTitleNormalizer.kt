package com.example.service.intelligence.smart

import java.util.regex.Pattern

/**
 * Step 83: Smart Title Normalization.
 * 
 * Normalizes titles for robust duplicate and update detection:
 * - Handles word order variations (e.g. "SSC CGL 2026 Recruitment" vs "SSC CGL Recruitment 2026").
 * - Strips noisy action banners ("Online Form Out", "Notification Released", "Apply Now", "Direct Link").
 * - Preserves critical qualifiers, years, and specific posts so distinct recruitments are NEVER accidentally merged.
 */
object SmartTitleNormalizer {

    private val NOISE_WORDS = listOf(
        "notification out", "notification released", "online form", "apply online",
        "official notification", "direct recruitment", "apply now",
        "अधिसूचना जारी", "ऑनलाइन आवेदन", "सीधी भर्ती", "आवेदन शुरू", "फॉर्म शुरू",
        "recruitment", "notification", "vacancy", "vacancies", "bharti", "post", "posts",
        "online", "form", "apply", "released", "out", "direct", "notice"
    )

    private val PUNCTUATION_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s\u0900-\u097F]")

    /**
     * Produces a standardized, canonical key representation of a title for matching.
     */
    fun normalizeTitle(title: String): String {
        if (title.isBlank()) return ""

        var clean = title.lowercase()

        // 1. Remove non-alphanumeric punctuation (except spaces and Hindi unicode chars)
        clean = PUNCTUATION_PATTERN.matcher(clean).replaceAll(" ")

        // 2. Remove standard notification boilerplate phrases and noise words
        for (noise in NOISE_WORDS) {
            clean = clean.replace(Regex("\\b$noise\\b", RegexOption.IGNORE_CASE), " ")
        }

        // 3. Extract tokens and reorder them deterministically while keeping year and core acronyms
        val tokens = clean.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 1 }
            .sorted()

        return tokens.joinToString(" ").trim()
    }

    /**
     * Computes similarity between two titles (0.0 to 1.0) using token set overlap.
     */
    fun calculateTitleSimilarity(titleA: String, titleB: String): Float {
        val normA = normalizeTitle(titleA)
        val normB = normalizeTitle(titleB)

        if (normA.isEmpty() || normB.isEmpty()) return 0.0f
        if (normA == normB) return 1.0f

        val tokensA = normA.split(" ").toSet()
        val tokensB = normB.split(" ").toSet()

        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size

        return if (union == 0) 0.0f else intersection.toFloat() / union.toFloat()
    }
}
