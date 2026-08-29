package com.example.data.model.updates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.RecruitmentContentType
import com.example.data.model.RecruitmentEntity
import com.example.data.model.VacancyStatus
import com.example.ui.theme.*
import java.util.UUID

enum class UpdateCategory(
    val key: String,
    val titleEn: String,
    val titleHi: String,
    val subtitleEn: String,
    val subtitleHi: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badgeTextEn: String,
    val badgeTextHi: String
) {
    VACANCY(
        key = "vacancy",
        titleEn = "Vacancy",
        titleHi = "सरकारी भर्तियां",
        subtitleEn = "Govt Jobs, Recruitment & Online Forms",
        subtitleHi = "सरकारी नौकरियां, नई भर्तियां व आवेदन",
        icon = Icons.Filled.WorkOutline,
        accentColor = Color(0xFF10B981), // Emerald
        badgeTextEn = "Active Jobs",
        badgeTextHi = "सक्रिय नौकरियां"
    ),
    ADMIT_CARD(
        key = "admit_card",
        titleEn = "Admit Card",
        titleHi = "एडमिट कार्ड",
        subtitleEn = "Hall Tickets, City Slips & Schedules",
        subtitleHi = "प्रवेश पत्र, परीक्षा शहर पर्ची व शेड्यूल",
        icon = Icons.Filled.ConfirmationNumber,
        accentColor = Color(0xFF3B82F6), // Blue
        badgeTextEn = "Hall Tickets",
        badgeTextHi = "प्रवेश पत्र"
    ),
    RESULT(
        key = "result",
        titleEn = "Result",
        titleHi = "परीक्षा परिणाम",
        subtitleEn = "Scorecards, Cutoff Marks & Merit Lists",
        subtitleHi = "स्कोरकार्ड, कटऑफ मार्क्स व मेरिट सूची",
        icon = Icons.Filled.EmojiEvents,
        accentColor = Color(0xFF8B5CF6), // Purple
        badgeTextEn = "Scorecards",
        badgeTextHi = "परिणाम"
    ),
    ANSWER_KEY(
        key = "answer_key",
        titleEn = "Answer Key",
        titleHi = "उत्तर कुंजी",
        subtitleEn = "Provisional Keys, Solutions & Objections",
        subtitleHi = "आंसर की, हल प्रश्नपत्र व आपत्ति लिंक",
        icon = Icons.Filled.Key,
        accentColor = Color(0xFFF59E0B), // Amber
        badgeTextEn = "Answer Keys",
        badgeTextHi = "उत्तर कुंजी"
    ),
    ADMISSION(
        key = "admission",
        titleEn = "Admission",
        titleHi = "प्रवेश सूचना",
        subtitleEn = "University Forms, Counseling & Entrances",
        subtitleHi = "विश्वविद्यालय प्रवेश, काउंसलिंग व फॉर्म",
        icon = Icons.Filled.School,
        accentColor = Color(0xFFEC4899), // Pink/Magenta
        badgeTextEn = "Admissions",
        badgeTextHi = "प्रवेश"
    );

    companion object {
        fun fromKey(key: String?): UpdateCategory {
            if (key == null) return VACANCY
            val cleanKey = key.trim().lowercase()
            return when {
                cleanKey.contains("admit") || cleanKey.contains("hall") -> ADMIT_CARD
                cleanKey.contains("result") || cleanKey.contains("score") -> RESULT
                cleanKey.contains("answer") || cleanKey.contains("key") -> ANSWER_KEY
                cleanKey.contains("admission") || cleanKey.contains("counseling") || cleanKey.contains("entrance") -> ADMISSION
                else -> entries.find { it.key.equals(cleanKey, ignoreCase = true) } ?: VACANCY
            }
        }
    }
}

data class LatestUpdateItem(
    val id: String = UUID.randomUUID().toString(),
    val updateType: String = "vacancy", // vacancy, admit_card, result, answer_key, admission
    val title: String = "",
    val shortDescription: String = "",
    val fullContent: String = "",
    val organization: String = "",
    val examName: String = "",
    val postName: String = "",
    val publishedDate: String? = null,
    val startDate: String? = null,
    val lastDate: String? = null,
    val examDate: String? = null,
    val sourceUrl: String = "",
    val applyUrl: String = "",
    val downloadUrl: String = "",
    val imageUrl: String = "",
    val language: String = "English",
    val sourceName: String = "Official Portal",
    val sourceType: String = "OFFICIAL_PORTAL",
    val externalId: String? = null,
    val contentHash: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Enhanced Rich Details
    val totalVacancies: Int? = null,
    val feeDetails: String = "Not specified",
    val educationalQualification: String = "Not specified",
    val ageCriteria: String = "Not specified",
    val selectionProcess: List<String> = emptyList(),
    val importantInstructions: List<String> = emptyList(),
    val isSaved: Boolean = false,
    val hasDeadlineReminder: Boolean = false
) {
    val category: UpdateCategory get() = UpdateCategory.fromKey(updateType)

    fun toRecruitmentEntity(): RecruitmentEntity {
        val contentTypeStr = when (category) {
            UpdateCategory.VACANCY -> RecruitmentContentType.VACANCY.name
            UpdateCategory.ADMIT_CARD -> RecruitmentContentType.ADMIT_CARD.name
            UpdateCategory.RESULT -> RecruitmentContentType.RESULT.name
            UpdateCategory.ANSWER_KEY -> RecruitmentContentType.ANSWER_KEY.name
            UpdateCategory.ADMISSION -> RecruitmentContentType.NOTIFICATION.name
        }

        return RecruitmentEntity(
            id = id,
            title = title,
            organization = organization.ifBlank { "Government Organization" },
            postName = postName.ifBlank { examName }.ifBlank { "Various Posts" },
            contentType = contentTypeStr,
            rawStatus = VacancyStatus.OPEN.name,
            totalVacancies = totalVacancies,
            applicationStartDate = startDate ?: publishedDate,
            applicationLastDate = lastDate,
            examDate = examDate,
            admitCardDate = if (category == UpdateCategory.ADMIT_CARD) publishedDate else null,
            resultDate = if (category == UpdateCategory.RESULT) publishedDate else null,
            feeDetails = feeDetails,
            educationalQualification = educationalQualification,
            ageRelaxation = ageCriteria,
            selectionProcess = selectionProcess,
            sourceUrl = sourceUrl.ifBlank { "https://www.sarkariresult.com/" },
            officialSourceUrl = sourceUrl,
            applicationUrl = applyUrl.ifBlank { downloadUrl },
            officialPdfUrl = downloadUrl,
            summaryEn = shortDescription,
            summaryHi = shortDescription,
            whatShouldIDo = importantInstructions,
            isVerified = true,
            isSaved = isSaved,
            hasDeadlineReminder = hasDeadlineReminder
        )
    }

    companion object {
        fun fromRecruitmentEntity(entity: RecruitmentEntity): LatestUpdateItem {
            val uType = when (entity.contentType) {
                RecruitmentContentType.ADMIT_CARD.name -> "admit_card"
                RecruitmentContentType.RESULT.name -> "result"
                RecruitmentContentType.ANSWER_KEY.name -> "answer_key"
                RecruitmentContentType.NOTIFICATION.name -> {
                    if (entity.title.contains("Admission", ignoreCase = true) || entity.title.contains("प्रवेश", ignoreCase = true)) "admission"
                    else "vacancy"
                }
                else -> "vacancy"
            }

            return LatestUpdateItem(
                id = entity.id,
                updateType = uType,
                title = entity.title,
                shortDescription = entity.summaryEn.ifBlank { entity.summaryHi },
                fullContent = entity.summaryEn.ifBlank { entity.summaryHi },
                organization = entity.organization,
                examName = entity.examCategory,
                postName = entity.postName,
                publishedDate = entity.applicationStartDate ?: entity.admitCardDate ?: entity.resultDate,
                startDate = entity.applicationStartDate,
                lastDate = entity.applicationLastDate,
                examDate = entity.examDate,
                sourceUrl = entity.officialSourceUrl.ifBlank { entity.sourceUrl },
                applyUrl = entity.applicationUrl,
                downloadUrl = entity.officialPdfUrl.ifBlank { entity.applicationUrl },
                totalVacancies = entity.totalVacancies,
                feeDetails = entity.feeDetails,
                educationalQualification = entity.educationalQualification,
                ageCriteria = entity.ageRelaxation,
                selectionProcess = entity.selectionProcess,
                importantInstructions = entity.whatShouldIDo,
                isSaved = entity.isSaved,
                hasDeadlineReminder = entity.hasDeadlineReminder,
                createdAt = entity.fetchedAt,
                updatedAt = entity.lastVerifiedAt
            )
        }
    }
}

data class CategoryFeedState(
    val items: List<LatestUpdateItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedOrg: String = "",
    val selectedExam: String = "",
    val selectedSort: String = "NEWEST",
    val currentPage: Int = 0,
    val hasMorePages: Boolean = true
)

