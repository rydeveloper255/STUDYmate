package com.example.service.intelligence.smart

import java.util.regex.Pattern

/**
 * Step 83: Smart Category Data Extractor.
 * 
 * Extracts category-specific structured attributes with absolute strictness:
 * - Zero hallucination / zero invention invariant: Unmentioned fields remain strictly NULL.
 * - Extracts organization/institution from recognized boards or contextual headers.
 * - Extracts vacancy count, qualification, age limit, application fee when explicitly present.
 * - Integrates with SmartDateIntelligence and SmartLinkClassifier.
 */
object SmartCategoryDataExtractor {

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
        "Police" to "State Police Recruitment Board",
        "AIIMS" to "All India Institute of Medical Sciences (AIIMS)",
        "NHM" to "National Health Mission (NHM)",
        "KVS" to "Kendriya Vidyalaya Sangathan (KVS)",
        "NVS" to "Navodaya Vidyalaya Samiti (NVS)"
    )

    private val VACANCY_COUNT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:total\\s*vacanc(?:y|ies)|total\\s*posts?|कुल\\s*पद|पदों\\s*की\\s*संख्या)[:\\s]*([0-9,]+)"),
        Pattern.compile("(?i)\\b([0-9,]+)\\s+(?:posts?|vacancies|पद)\\b")
    )

    private val QUALIFICATION_PATTERNS = listOf(
        Pattern.compile("(?i)(?:qualification|eligibility|योग्यता|शैक्षणिक\\s*योग्यता)[:\\s]*([^\\n]+)"),
        Pattern.compile("(?i)\\b(10th(?:\\s*Pass)?|12th(?:\\s*Pass)?|Graduate|Post\\s*Graduate|Diploma|B\\.Tech|B\\.Ed|D\\.El\\.Ed|B\\.Sc|ITI)\\b")
    )

    private val AGE_LIMIT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:age\\s*limit|आयु\\s*सीमा|age)[:\\s]*([^\\n]+)"),
        Pattern.compile("(?i)\\b([0-9]{2}\\s*-\\s*[0-9]{2}\\s*(?:years|वर्ष|yrs))\\b")
    )

    private val FEE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:application\\s*fee|exam\\s*fee|फीस|आवेदन\\s*शुल्क)[:\\s]*([^\\n]+)"),
        Pattern.compile("(?i)(?:General/OBC|UR/OBC)[:\\s]*₹?\\s*([0-9]+)")
    )

    private val POST_NAME_PATTERNS = listOf(
        Pattern.compile("(?i)(?:post\\s*name|पद\\s*का\\s*नाम|name\\s*of\\s*post)[:\\s]*([^\\n]+)")
    )

    private val COURSE_PATTERNS = listOf(
        Pattern.compile("(?i)(?:course|program|पाठ्यक्रम)[:\\s]*([^\\n]+)"),
        Pattern.compile("(?i)\\b(B\\.A|B\\.Sc|B\\.Com|B\\.Tech|M\\.A|M\\.Sc|M\\.Com|M\\.Tech|B\\.Ed|D\\.El\\.Ed|LLB|LLM|MBBS|BDS|B\\.Pharma)\\b")
    )

    /**
     * Extracts structured category-specific data without inventing facts.
     */
    fun extractData(
        title: String,
        text: String,
        category: SmartContentCategory,
        sourceUrl: String,
        sourcePostDate: String?,
        links: List<SmartLink>,
        dates: SmartDateIntelligence.ExtractedDates
    ): SmartExtractedData {
        val organization = extractOrganization(title, text)
        val officialUrl = links.firstOrNull { it.linkType == SmartLinkType.OFFICIAL }?.url
        val pdfUrl = links.firstOrNull { it.linkType == SmartLinkType.PDF }?.url

        return when (category) {
            SmartContentCategory.VACANCY -> {
                val applyUrl = links.firstOrNull { it.linkType == SmartLinkType.APPLY }?.url
                val vacancyCount = extractVacancyCount(text)
                val qualification = extractQualification(text)
                val ageLimit = extractAgeLimit(text)
                val fee = extractFee(text)
                val postName = extractPostName(title, text, organization)

                SmartExtractedData.Vacancy(
                    SmartVacancyData(
                        title = title,
                        organization = organization,
                        postName = postName,
                        vacancyCount = vacancyCount,
                        qualification = qualification,
                        ageLimit = ageLimit,
                        applicationStartDate = dates.startDate,
                        lastDate = dates.lastDate,
                        examDate = dates.examDate,
                        applicationFee = fee,
                        officialNotificationUrl = pdfUrl ?: officialUrl,
                        applyUrl = applyUrl ?: officialUrl,
                        sourceUrl = sourceUrl,
                        sourcePostDate = sourcePostDate,
                        isLastDateExtended = dates.isLastDateExtended,
                        originalLastDate = dates.originalLastDate
                    )
                )
            }

            SmartContentCategory.RESULT -> {
                val resultUrl = links.firstOrNull { it.linkType == SmartLinkType.RESULT }?.url
                SmartExtractedData.Result(
                    SmartResultData(
                        title = title,
                        organization = organization,
                        examName = extractExamName(title, organization),
                        resultDate = dates.resultDate ?: dates.startDate,
                        examDate = dates.examDate,
                        resultUrl = resultUrl ?: officialUrl,
                        officialUrl = officialUrl,
                        sourceUrl = sourceUrl,
                        sourcePostDate = sourcePostDate
                    )
                )
            }

            SmartContentCategory.ADMIT_CARD -> {
                val downloadUrl = links.firstOrNull { it.linkType == SmartLinkType.ADMIT_CARD }?.url ?: pdfUrl
                SmartExtractedData.AdmitCard(
                    SmartAdmitCardData(
                        title = title,
                        organization = organization,
                        examName = extractExamName(title, organization),
                        examDate = dates.examDate,
                        admitCardReleaseDate = dates.admitCardDate ?: dates.startDate,
                        downloadUrl = downloadUrl ?: officialUrl,
                        officialUrl = officialUrl,
                        sourceUrl = sourceUrl,
                        sourcePostDate = sourcePostDate
                    )
                )
            }

            SmartContentCategory.ANSWER_KEY -> {
                val answerKeyUrl = links.firstOrNull { it.linkType == SmartLinkType.ANSWER_KEY }?.url ?: pdfUrl
                SmartExtractedData.AnswerKey(
                    SmartAnswerKeyData(
                        title = title,
                        organization = organization,
                        examName = extractExamName(title, organization),
                        answerKeyDate = dates.answerKeyDate ?: dates.startDate,
                        examDate = dates.examDate,
                        answerKeyUrl = answerKeyUrl ?: officialUrl,
                        officialUrl = officialUrl,
                        sourceUrl = sourceUrl,
                        sourcePostDate = sourcePostDate
                    )
                )
            }

            SmartContentCategory.ADMISSION -> {
                val applicationUrl = links.firstOrNull { it.linkType == SmartLinkType.ADMISSION || it.linkType == SmartLinkType.APPLY }?.url
                val course = extractCourse(text)
                val qualification = extractQualification(text)
                SmartExtractedData.Admission(
                    SmartAdmissionData(
                        title = title,
                        institution = organization,
                        course = course,
                        qualification = qualification,
                        applicationStartDate = dates.startDate,
                        lastDate = dates.lastDate,
                        admissionDate = dates.admissionDate,
                        applicationUrl = applicationUrl ?: officialUrl,
                        officialUrl = officialUrl,
                        sourceUrl = sourceUrl,
                        sourcePostDate = sourcePostDate
                    )
                )
            }

            SmartContentCategory.OTHER -> {
                SmartExtractedData.Other(
                    SmartOtherData(
                        title = title,
                        organization = organization,
                        noticeDate = dates.startDate ?: sourcePostDate,
                        officialUrl = officialUrl ?: links.firstOrNull()?.url,
                        sourceUrl = sourceUrl,
                        sourcePostDate = sourcePostDate
                    )
                )
            }
        }
    }

    fun extractOrganization(title: String, fullText: String): String? {
        val combined = "$title\n$fullText"
        for ((shortName, fullName) in KNOWN_ORGS) {
            val p = Pattern.compile("\\b$shortName\\b", Pattern.CASE_INSENSITIVE)
            if (p.matcher(combined).find()) {
                return fullName
            }
        }
        return null
    }

    private fun extractExamName(title: String, org: String?): String {
        var clean = title
        if (!org.isNullOrBlank()) {
            clean = clean.replace(org, "", ignoreCase = true)
        }
        return clean.replace("Admit Card", "", ignoreCase = true)
            .replace("Result", "", ignoreCase = true)
            .replace("Answer Key", "", ignoreCase = true)
            .replace("Declared", "", ignoreCase = true)
            .replace("Released", "", ignoreCase = true)
            .replace("Out", "", ignoreCase = true)
            .trim()
            .ifBlank { title }
    }

    private fun extractPostName(title: String, text: String, org: String?): String? {
        for (pattern in POST_NAME_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val group = m.group(1)?.trim()
                if (!group.isNullOrBlank()) return group
            }
        }
        return null
    }

    private fun extractVacancyCount(text: String): Int? {
        for (pattern in VACANCY_COUNT_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val raw = m.group(1)?.replace(",", "")?.trim()
                val parsed = raw?.toIntOrNull()
                if (parsed != null && parsed > 0) return parsed
            }
        }
        return null
    }

    private fun extractQualification(text: String): String? {
        for (pattern in QUALIFICATION_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val group = m.group(1)?.trim()
                if (!group.isNullOrBlank()) return group
            }
        }
        return null
    }

    private fun extractAgeLimit(text: String): String? {
        for (pattern in AGE_LIMIT_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val group = m.group(1)?.trim()
                if (!group.isNullOrBlank()) return group
            }
        }
        return null
    }

    private fun extractFee(text: String): String? {
        for (pattern in FEE_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val group = m.group(1)?.trim()
                if (!group.isNullOrBlank()) return group
            }
        }
        return null
    }

    private fun extractCourse(text: String): String? {
        for (pattern in COURSE_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val group = m.group(1)?.trim()
                if (!group.isNullOrBlank()) return group
            }
        }
        return null
    }
}
