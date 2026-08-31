package com.example.service.content.whatsapp

import com.example.data.model.RecruitmentContentType
import com.example.data.model.RecruitmentEntity
import com.example.data.model.VacancyStatus
import com.example.data.model.updates.LatestUpdateItem
import com.example.data.model.updates.UpdateCategory
import java.util.UUID

/**
 * Supported initial category classifications for WhatsApp updates.
 * Extensible for advanced AI classification in future steps.
 */
enum class WhatsAppCategory(val key: String, val displayName: String, val updateCategory: UpdateCategory) {
    VACANCY("vacancy", "Vacancy", UpdateCategory.VACANCY),
    RESULT("result", "Result", UpdateCategory.RESULT),
    ADMIT_CARD("admit_card", "Admit Card", UpdateCategory.ADMIT_CARD),
    ANSWER_KEY("answer_key", "Answer Key", UpdateCategory.ANSWER_KEY),
    ADMISSION("admission", "Admission", UpdateCategory.ADMISSION),
    OTHER("other", "Other Updates", UpdateCategory.VACANCY);

    companion object {
        fun fromKey(key: String?): WhatsAppCategory {
            if (key == null) return OTHER
            val clean = key.trim().lowercase()
            return entries.find { it.key == clean } ?: OTHER
        }
    }
}

/**
 * Structured content and operational ingestion lifecycle status.
 */
enum class WhatsAppIngestionStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    UPDATED,
    DUPLICATE,
    EXPIRED,
    FAILED,
    REVIEW_REQUIRED,
    DATA_CONFLICT,
    CATEGORY_CONFLICT,
    DATE_CONFLICT,
    DATE_UNAVAILABLE,
    SOURCE_UNAVAILABLE
}

/**
 * Classification of links extracted from source posts.
 */
enum class ExtractedLinkType(val label: String) {
    OFFICIAL_WEBSITE("Official Website"),
    APPLY_LINK("Apply Online"),
    PDF_LINK("Official Notification PDF"),
    RESULT_LINK("Result & Scorecard"),
    ANSWER_KEY_LINK("Answer Key & Objection"),
    OTHER_LINK("Direct Link")
}

data class ExtractedLink(
    val url: String,
    val linkType: ExtractedLinkType,
    val displayText: String
)

data class ExtractedAttachment(
    val attachmentType: String, // "PDF", "IMAGE", "DOCUMENT"
    val reference: String,
    val url: String? = null,
    val fileSize: Long? = null
)

/**
 * Raw representation of incoming WhatsApp channel post before processing.
 * Preserves source integrity with zero fake information.
 */
data class WhatsAppRawPost(
    val id: String = UUID.randomUUID().toString(),
    val sourceType: String = WhatsAppSourceConfig.SOURCE_TYPE,
    val sourceUrl: String = WhatsAppSourceConfig.configuredChannelUrl,
    val sourceMessageId: String? = null,
    val sourcePostDate: String? = null,
    val rawText: String,
    val attachmentReference: String? = null,
    val sourceTimestamp: Long? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
    val status: WhatsAppIngestionStatus = WhatsAppIngestionStatus.PENDING
)

/**
 * Extracted, structured, verified content entity parsed from WhatsApp source.
 */
data class WhatsAppProcessedContent(
    val id: String = UUID.randomUUID().toString(),
    val sourceMessageId: String? = null,
    val title: String,
    val category: WhatsAppCategory,
    val description: String,
    val sourceUrl: String = WhatsAppSourceConfig.configuredChannelUrl,
    val officialUrl: String? = null,
    val applyUrl: String? = null,
    val pdfUrl: String? = null,
    val resultUrl: String? = null,
    val postDate: String? = null,
    val lastDate: String? = null,
    val examDate: String? = null,
    val organization: String? = null,
    val postName: String? = null,
    val totalVacancies: Int? = null,
    val feeDetails: String? = null,
    val qualification: String? = null,
    val ageCriteria: String? = null,
    val selectionProcess: List<String> = emptyList(),
    val importantInstructions: List<String> = emptyList(),
    val sourceType: String = WhatsAppSourceConfig.SOURCE_TYPE,
    val sourceName: String = WhatsAppSourceConfig.SOURCE_NAME,
    val status: WhatsAppIngestionStatus = WhatsAppIngestionStatus.PROCESSING,
    val links: List<ExtractedLink> = emptyList(),
    val attachments: List<ExtractedAttachment> = emptyList(),
    val contentHash: String = "",
    val fetchedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    /**
     * Converts to canonical LatestUpdateItem for UI display and Supabase persistence.
     */
    fun toLatestUpdateItem(): LatestUpdateItem {
        val metaMap = mutableMapOf<String, String>()
        metaMap["source_type"] = sourceType
        metaMap["source_name"] = sourceName
        sourceMessageId?.let { metaMap["source_message_id"] = it }
        totalVacancies?.let { metaMap["total_vacancies"] = it.toString() }
        feeDetails?.let { metaMap["fee_details"] = it }
        qualification?.let { metaMap["qualification"] = it }
        ageCriteria?.let { metaMap["age_limit"] = it }
        metaMap["ingestion_status"] = status.name

        return LatestUpdateItem(
            id = id,
            updateType = category.key,
            title = title,
            shortDescription = description,
            fullContent = description,
            organization = organization ?: "Official Board",
            examName = organization ?: "",
            postName = postName ?: "",
            publishedDate = postDate,
            startDate = postDate,
            lastDate = lastDate,
            examDate = examDate,
            sourceUrl = sourceUrl,
            applyUrl = applyUrl ?: officialUrl ?: "",
            downloadUrl = pdfUrl ?: resultUrl ?: "",
            sourceName = sourceName,
            sourceType = sourceType,
            externalId = sourceMessageId,
            contentHash = contentHash,
            metadata = metaMap,
            isActive = isActive && status == WhatsAppIngestionStatus.PUBLISHED,
            createdAt = createdAt,
            updatedAt = updatedAt,
            totalVacancies = totalVacancies,
            feeDetails = feeDetails ?: "Refer to official notice",
            educationalQualification = qualification ?: "Refer to official notice",
            ageCriteria = ageCriteria ?: "Refer to official notice",
            selectionProcess = selectionProcess,
            importantInstructions = importantInstructions
        )
    }

    /**
     * Converts to canonical local Room RecruitmentEntity for offline persistence and unified tracking.
     */
    fun toRecruitmentEntity(): RecruitmentEntity {
        val cType = when (category) {
            WhatsAppCategory.VACANCY -> RecruitmentContentType.VACANCY.name
            WhatsAppCategory.ADMIT_CARD -> RecruitmentContentType.ADMIT_CARD.name
            WhatsAppCategory.RESULT -> RecruitmentContentType.RESULT.name
            WhatsAppCategory.ANSWER_KEY -> RecruitmentContentType.ANSWER_KEY.name
            WhatsAppCategory.ADMISSION -> RecruitmentContentType.NOTIFICATION.name
            WhatsAppCategory.OTHER -> RecruitmentContentType.NOTIFICATION.name
        }

        return RecruitmentEntity(
            id = id,
            title = title,
            organization = organization ?: "Government Organization",
            postName = postName ?: title,
            contentType = cType,
            rawStatus = if (status == WhatsAppIngestionStatus.EXPIRED) VacancyStatus.CLOSED.name else VacancyStatus.OPEN.name,
            totalVacancies = totalVacancies,
            applicationStartDate = postDate,
            applicationLastDate = lastDate,
            examDate = examDate,
            feeDetails = feeDetails ?: "Not specified",
            educationalQualification = qualification ?: "Not specified",
            ageRelaxation = ageCriteria ?: "Not specified",
            selectionProcess = selectionProcess,
            sourceUrl = sourceUrl,
            officialSourceUrl = officialUrl ?: sourceUrl,
            applicationUrl = applyUrl ?: officialUrl ?: "",
            officialPdfUrl = pdfUrl ?: "",
            summaryEn = description,
            summaryHi = description,
            whatShouldIDo = importantInstructions,
            isVerified = true,
            fetchedAt = fetchedAt,
            lastVerifiedAt = updatedAt,
            contentHash = contentHash
        )
    }
}

/**
 * Pipeline audit log stages for traceable, secure observability.
 */
enum class WhatsAppIngestionStage {
    FETCH_STARTED,
    FETCH_SUCCESS,
    FETCH_FAILED,
    PROCESSING_STARTED,
    CLASSIFICATION_RESULT,
    VALIDATION_RESULT,
    DUPLICATE_DETECTED,
    SAVED,
    PUBLISHED,
    FAILED
}

data class WhatsAppIngestionLog(
    val stage: WhatsAppIngestionStage,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, String> = emptyMap()
)
