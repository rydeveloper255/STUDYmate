package com.example.service.intelligence.smart

/**
 * Step 83: Smart Summary Generator.
 * 
 * Generates concise, 1-line structured summaries containing strictly source-verified facts.
 * Absolutely no embellishments, clickbait, or unverified claims.
 */
object SmartSummaryGenerator {

    /**
     * Creates a high-clarity summary line based strictly on extracted category attributes.
     */
    fun generateSummary(
        category: SmartContentCategory,
        extractedData: SmartExtractedData,
        lastDate: String?,
        examDate: String?
    ): String {
        val parts = mutableListOf<String>()

        val org = extractedData.organization
        if (!org.isNullOrBlank()) {
            parts.add(org)
        }

        when (extractedData) {
            is SmartExtractedData.Vacancy -> {
                val data = extractedData.data
                parts.add(data.title)
                if (data.vacancyCount != null && data.vacancyCount > 0) {
                    parts.add("${"%,d".format(data.vacancyCount)} Posts")
                }
                if (!data.lastDate.isNullOrBlank()) {
                    if (data.isLastDateExtended) {
                        parts.add("Last Date Extended: ${data.lastDate}")
                    } else {
                        parts.add("Apply before ${data.lastDate}")
                    }
                }
            }

            is SmartExtractedData.Result -> {
                val data = extractedData.data
                parts.add(data.title)
                if (!data.resultDate.isNullOrBlank()) {
                    parts.add("Result Declared: ${data.resultDate}")
                }
            }

            is SmartExtractedData.AdmitCard -> {
                val data = extractedData.data
                parts.add(data.title)
                if (!data.examDate.isNullOrBlank()) {
                    parts.add("Exam Date: ${data.examDate}")
                }
                if (!data.admitCardReleaseDate.isNullOrBlank()) {
                    parts.add("Released: ${data.admitCardReleaseDate}")
                }
            }

            is SmartExtractedData.AnswerKey -> {
                val data = extractedData.data
                parts.add(data.title)
                if (!data.answerKeyDate.isNullOrBlank()) {
                    parts.add("Answer Key Released: ${data.answerKeyDate}")
                }
            }

            is SmartExtractedData.Admission -> {
                val data = extractedData.data
                parts.add(data.title)
                if (!data.course.isNullOrBlank()) {
                    parts.add(data.course)
                }
                if (!data.lastDate.isNullOrBlank()) {
                    parts.add("Last Date: ${data.lastDate}")
                }
            }

            is SmartExtractedData.Other -> {
                val data = extractedData.data
                parts.add(data.title)
            }
        }

        return parts.distinct().joinToString(" — ").ifBlank { extractedData.title }
    }
}
