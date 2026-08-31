package com.example.service.intelligence.smart

/**
 * Step 83: Smart Category Classifier.
 * 
 * Performs deep, context-based category classification with confidence scoring:
 * - Differentiates lifecycle events (e.g. "SSC CGL Recruitment Admit Card" -> ADMIT_CARD, not VACANCY).
 * - Distinguishes Results, Scorecards, Merit Lists -> RESULT.
 * - Distinguishes Answer Keys, Response Sheets, Objection Windows -> ANSWER_KEY.
 * - Distinguishes College / University Admissions, Counseling, Allotments -> ADMISSION.
 * - Distinguishes Job Notifications, Online Applications, Bharti -> VACANCY.
 * - Calculates confidence score (0.0 to 1.0) and confidence level.
 * - Flags ambiguous / low-confidence posts for human review (REVIEW_REQUIRED).
 */
object SmartCategoryClassifier {

    const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
    const val MEDIUM_CONFIDENCE_THRESHOLD = 0.60f

    private val ANSWER_KEY_SIGNALS = listOf(
        "answer key" to 0.40f, "answer-key" to 0.40f, "उत्तर कुंजी" to 0.45f,
        "उत्तरमाला" to 0.45f, "response sheet" to 0.35f, "रिस्पॉन्स शीट" to 0.35f,
        "provisional key" to 0.35f, "tentative key" to 0.35f, "final answer key" to 0.40f,
        "objection link" to 0.35f, "objection tracker" to 0.35f, "आपत्ति दर्ज" to 0.35f,
        "challenge answer key" to 0.35f, "master question paper" to 0.30f
    )

    private val ADMIT_CARD_SIGNALS = listOf(
        "admit card" to 0.40f, "admit-card" to 0.40f, "एडमिट कार्ड" to 0.45f,
        "hall ticket" to 0.40f, "हॉल टिकट" to 0.40f, "call letter" to 0.35f,
        "कॉल लेटर" to 0.35f, "city intimation" to 0.40f, "city slip" to 0.35f,
        "exam city" to 0.30f, "परीक्षा शहर" to 0.35f, "e-admit card" to 0.40f,
        "download admit card" to 0.45f, "प्रवेश पत्र" to 0.40f
    )

    private val RESULT_SIGNALS = listOf(
        "result" to 0.40f, "रिजल्ट" to 0.45f, "परिणाम" to 0.45f,
        "scorecard" to 0.40f, "score card" to 0.40f, "स्कोरकार्ड" to 0.40f,
        "marksheet" to 0.35f, "mark sheet" to 0.35f, "merit list" to 0.40f,
        "मेरिट लिस्ट" to 0.40f, "cut off" to 0.30f, "cutoff" to 0.30f, "कटऑफ" to 0.30f,
        "qualified candidates" to 0.35f, "final selection" to 0.35f,
        "चयनित सूची" to 0.40f, "tier 1 result" to 0.45f, "tier 2 result" to 0.45f,
        "prelims result" to 0.45f, "mains result" to 0.45f, "result declared" to 0.45f
    )

    private val ADMISSION_SIGNALS = listOf(
        "admission" to 0.40f, "प्रवेश" to 0.35f, "counseling" to 0.40f,
        "counselling" to 0.40f, "काउंसलिंग" to 0.40f, "seat allotment" to 0.40f,
        "सीट आवंटन" to 0.40f, "entrance exam" to 0.30f, "प्रवेश परीक्षा" to 0.35f,
        "cuet ug" to 0.35f, "cuet pg" to 0.35f, "csas" to 0.35f, "bed admission" to 0.40f,
        "deled admission" to 0.40f, "polytechnic admission" to 0.40f,
        "ignou admission" to 0.40f, "university admission" to 0.40f, "admission form" to 0.45f
    )

    private val VACANCY_SIGNALS = listOf(
        "recruitment" to 0.35f, "vacancy" to 0.35f, "vacancies" to 0.35f,
        "bharti" to 0.35f, "भर्ती" to 0.35f, "रिक्ति" to 0.35f, "नौकरी" to 0.30f,
        "job" to 0.25f, "posts" to 0.25f, "total posts" to 0.35f, "कुल पद" to 0.35f,
        "apply online" to 0.40f, "online form" to 0.35f, "आवेदन शुरू" to 0.35f,
        "notification out" to 0.30f, "अधिसूचना" to 0.30f, "direct recruitment" to 0.35f,
        "last date" to 0.20f, "eligibility" to 0.20f, "qualification" to 0.20f
    )

    data class ClassificationResult(
        val category: SmartContentCategory,
        val confidence: SmartConfidenceScore,
        val matchedSignals: List<String>
    )

    /**
     * Contextually classifies title and body text into a category with a confidence score.
     */
    fun classify(title: String, text: String): ClassificationResult {
        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()
        val combined = "$lowerTitle\n$lowerText".trim()

        if (combined.isBlank()) {
            return ClassificationResult(
                category = SmartContentCategory.OTHER,
                confidence = SmartConfidenceScore(0.0f, SmartConfidenceLevel.LOW, emptyList(), "Empty or blank content"),
                matchedSignals = emptyList()
            )
        }

        // Score each category with weight multiplier for title matches
        val scores = mutableMapOf<SmartContentCategory, Float>()
        val signals = mutableMapOf<SmartContentCategory, MutableList<String>>()

        fun evaluateCategory(
            category: SmartContentCategory,
            signalList: List<Pair<String, Float>>
        ) {
            var categoryScore = 0.0f
            val matched = mutableListOf<String>()

            for ((term, weight) in signalList) {
                val inTitle = lowerTitle.contains(term)
                val inBody = lowerText.contains(term)

                if (inTitle) {
                    categoryScore += weight * 1.5f // Higher weight if in title
                    matched.add("title:$term")
                } else if (inBody) {
                    categoryScore += weight
                    matched.add("body:$term")
                }
            }

            // Normalization clamp
            scores[category] = categoryScore.coerceAtMost(1.0f)
            signals[category] = matched
        }

        evaluateCategory(SmartContentCategory.ANSWER_KEY, ANSWER_KEY_SIGNALS)
        evaluateCategory(SmartContentCategory.ADMIT_CARD, ADMIT_CARD_SIGNALS)
        evaluateCategory(SmartContentCategory.RESULT, RESULT_SIGNALS)
        evaluateCategory(SmartContentCategory.ADMISSION, ADMISSION_SIGNALS)
        evaluateCategory(SmartContentCategory.VACANCY, VACANCY_SIGNALS)

        // Lifecycle Priority disambiguation:
        // If specific event signals (Admit card, Result, Answer key) exist in title or strong in body,
        // they take precedence over generic vacancy terms.
        val answerKeyScore = scores[SmartContentCategory.ANSWER_KEY] ?: 0f
        val admitCardScore = scores[SmartContentCategory.ADMIT_CARD] ?: 0f
        val resultScore = scores[SmartContentCategory.RESULT] ?: 0f
        val admissionScore = scores[SmartContentCategory.ADMISSION] ?: 0f
        val vacancyScore = scores[SmartContentCategory.VACANCY] ?: 0f

        val topCategory: SmartContentCategory
        val finalScore: Float
        val matchedList: List<String>

        when {
            answerKeyScore >= 0.35f && answerKeyScore >= admitCardScore && answerKeyScore >= resultScore -> {
                topCategory = SmartContentCategory.ANSWER_KEY
                finalScore = (answerKeyScore + 0.15f).coerceAtMost(0.99f)
                matchedList = signals[SmartContentCategory.ANSWER_KEY] ?: emptyList()
            }
            admitCardScore >= 0.35f && admitCardScore >= resultScore -> {
                topCategory = SmartContentCategory.ADMIT_CARD
                finalScore = (admitCardScore + 0.15f).coerceAtMost(0.99f)
                matchedList = signals[SmartContentCategory.ADMIT_CARD] ?: emptyList()
            }
            resultScore >= 0.35f -> {
                topCategory = SmartContentCategory.RESULT
                finalScore = (resultScore + 0.15f).coerceAtMost(0.99f)
                matchedList = signals[SmartContentCategory.RESULT] ?: emptyList()
            }
            admissionScore >= 0.35f && admissionScore >= vacancyScore -> {
                topCategory = SmartContentCategory.ADMISSION
                finalScore = (admissionScore + 0.15f).coerceAtMost(0.99f)
                matchedList = signals[SmartContentCategory.ADMISSION] ?: emptyList()
            }
            vacancyScore >= 0.25f -> {
                topCategory = SmartContentCategory.VACANCY
                finalScore = (vacancyScore + 0.10f).coerceAtMost(0.98f)
                matchedList = signals[SmartContentCategory.VACANCY] ?: emptyList()
            }
            else -> {
                topCategory = SmartContentCategory.OTHER
                finalScore = 0.40f
                matchedList = listOf("low_signal_classification")
            }
        }

        val level = when {
            finalScore >= HIGH_CONFIDENCE_THRESHOLD -> SmartConfidenceLevel.HIGH
            finalScore >= MEDIUM_CONFIDENCE_THRESHOLD -> SmartConfidenceLevel.MEDIUM
            else -> SmartConfidenceLevel.LOW
        }

        val reason = "Classified as ${topCategory.displayName} with score $finalScore based on ${matchedList.take(3).joinToString(", ")}"

        return ClassificationResult(
            category = topCategory,
            confidence = SmartConfidenceScore(
                score = finalScore,
                level = level,
                signals = matchedList,
                reason = reason
            ),
            matchedSignals = matchedList
        )
    }
}
