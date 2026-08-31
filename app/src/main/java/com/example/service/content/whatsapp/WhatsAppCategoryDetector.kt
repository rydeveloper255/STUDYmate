package com.example.service.content.whatsapp

/**
 * Step 82: Category Detection Foundation for WhatsApp Channel posts.
 * Provides deterministic classification across:
 * - VACANCY
 * - RESULT
 * - ADMIT_CARD
 * - ANSWER_KEY
 * - ADMISSION
 * - OTHER
 * 
 * Modular and architecture-ready for advanced AI classification in Step 83.
 */
object WhatsAppCategoryDetector {

    private val RESULT_KEYWORDS = listOf(
        "result", "रिजल्ट", "परिणाम", "scorecard", "score card", "स्कोरकार्ड",
        "merit list", "मेरिट लिस्ट", "cut off", "cutoff", "कटऑफ",
        "final selection", "चयनित सूची", "marks sheet", "qualified candidates"
    )

    private val ADMIT_CARD_KEYWORDS = listOf(
        "admit card", "admit-card", "एडमिट कार्ड", "hall ticket", "हॉल टिकट",
        "call letter", "कॉल लेटर", "city intimation", "city slip", "परीक्षा शहर",
        "e-admit card", "download admit card", "प्रवेश पत्र"
    )

    private val ANSWER_KEY_KEYWORDS = listOf(
        "answer key", "answer-key", "उत्तर कुंजी", "उत्तरमाला",
        "response sheet", "रिस्पॉन्स शीट", "provisional key", "tentative key",
        "objection link", "आपत्ति लिंक", "master question paper"
    )

    private val ADMISSION_KEYWORDS = listOf(
        "admission", "प्रवेश", "counseling", "counselling", "काउंसलिंग",
        "seat allotment", "सीट आवंटन", "entrance exam", "प्रवेश परीक्षा",
        "cuet", "csas", "ignou admission", "university admission", "bed admission"
    )

    private val VACANCY_KEYWORDS = listOf(
        "vacancy", "vacancies", "recruitment", "recruitment 2026", "recruitment 2027",
        "bharti", "bhartiya", "भर्ती", "रिक्ति", "नौकरी", "job", "posts", "पद",
        "online form", "apply online", "आवेदन", "अधिसूचना", "notification",
        "direct recruitment", "candidature", "application form"
    )

    /**
     * Detects category from title and body text with clear priority rules:
     * Specific event types (Result, Admit Card, Answer Key, Admission) take precedence
     * over generic Vacancy mentions if both occur (e.g. "SSC CGL Recruitment Exam Admit Card Released" -> ADMIT_CARD).
     */
    fun detectCategory(title: String, text: String): WhatsAppCategory {
        val classified = com.example.service.intelligence.smart.SmartCategoryClassifier.classify(title, text)
        return when (classified.category) {
            com.example.service.intelligence.smart.SmartContentCategory.VACANCY -> WhatsAppCategory.VACANCY
            com.example.service.intelligence.smart.SmartContentCategory.RESULT -> WhatsAppCategory.RESULT
            com.example.service.intelligence.smart.SmartContentCategory.ADMIT_CARD -> WhatsAppCategory.ADMIT_CARD
            com.example.service.intelligence.smart.SmartContentCategory.ANSWER_KEY -> WhatsAppCategory.ANSWER_KEY
            com.example.service.intelligence.smart.SmartContentCategory.ADMISSION -> WhatsAppCategory.ADMISSION
            com.example.service.intelligence.smart.SmartContentCategory.OTHER -> WhatsAppCategory.OTHER
        }
    }
}
