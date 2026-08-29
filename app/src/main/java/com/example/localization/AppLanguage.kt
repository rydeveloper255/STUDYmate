package com.example.localization

enum class AppLanguage(
    val code: String,
    val title: String,
    val nativeName: String,
    val shortCode: String
) {
    ENGLISH("en", "English", "English", "EN"),
    HINDI("hi", "Hindi", "हिन्दी", "हि");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return when (code?.lowercase()?.trim()) {
                "hi", "hindi", "हिंदी", "हिन्दी" -> HINDI
                else -> ENGLISH
            }
        }
    }
}
