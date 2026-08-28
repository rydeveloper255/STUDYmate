package com.example.service.collector

import com.example.data.model.content.ContentCategory
import com.example.data.model.content.ContentSourceConfig
import com.example.data.model.content.SourceStatus
import com.example.data.model.content.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized Source Manager configuration for StudyMate automated content collection.
 * Maintains runtime state, enable/disable toggles, priorities, health records, failure isolation, and backoff for all data sources.
 */
class SourceManager {

    companion object {
        const val SOURCE_ID_VACANCY = "src_sarkari_vacancy"
        const val SOURCE_ID_RESULT = "src_sarkari_result"
        const val SOURCE_ID_ADMIT_CARD = "src_sarkari_admit_card"
        const val SOURCE_ID_ANSWER_KEY = "src_sarkari_answer_key"
        const val SOURCE_ID_ADMISSION = "src_sarkari_admission"
        const val SOURCE_ID_WEEKLY_CA_PDF = "src_gknow_weekly_ca_pdf"
        const val SOURCE_ID_WHATSAPP_REF = "src_whatsapp_channel_ref"

        private const val UNHEALTHY_FAILURE_THRESHOLD = 3

        val DEFAULT_SOURCES = listOf(
            ContentSourceConfig(
                sourceId = SOURCE_ID_VACANCY,
                sourceName = "Sarkari Result — Latest Vacancies",
                sourceUrl = "https://sarkariresult.com.cm/latest-jobs/",
                category = ContentCategory.VACANCY,
                enabled = true,
                priority = 1,
                sourceType = SourceType.WEB_SCRAPER,
                language = "Hindi",
                description = "Primary discovery source for new recruitment vacancies and job notifications.",
                checkIntervalMinutes = 180L,
                status = SourceStatus.ACTIVE
            ),
            ContentSourceConfig(
                sourceId = SOURCE_ID_RESULT,
                sourceName = "Sarkari Result — Exam Results",
                sourceUrl = "https://sarkariresult.com.cm/result/",
                category = ContentCategory.RESULT,
                enabled = true,
                priority = 2,
                sourceType = SourceType.WEB_SCRAPER,
                language = "Hindi",
                description = "Government and competitive exam results announcement feed.",
                checkIntervalMinutes = 180L,
                status = SourceStatus.ACTIVE
            ),
            ContentSourceConfig(
                sourceId = SOURCE_ID_ADMIT_CARD,
                sourceName = "Sarkari Result — Admit Cards & Hall Tickets",
                sourceUrl = "https://sarkariresult.com.cm/admit-card/",
                category = ContentCategory.ADMIT_CARD,
                enabled = true,
                priority = 3,
                sourceType = SourceType.WEB_SCRAPER,
                language = "Hindi",
                description = "Exam admit cards, call letters, and exam city slip updates.",
                checkIntervalMinutes = 180L,
                status = SourceStatus.ACTIVE
            ),
            ContentSourceConfig(
                sourceId = SOURCE_ID_ANSWER_KEY,
                sourceName = "Sarkari Result — Answer Keys",
                sourceUrl = "https://sarkariresult.com.cm/answer-key/",
                category = ContentCategory.ANSWER_KEY,
                enabled = true,
                priority = 4,
                sourceType = SourceType.WEB_SCRAPER,
                language = "Hindi",
                description = "Provisional & final answer keys and objection windows.",
                checkIntervalMinutes = 180L,
                status = SourceStatus.ACTIVE
            ),
            ContentSourceConfig(
                sourceId = SOURCE_ID_ADMISSION,
                sourceName = "Sarkari Result — Admissions",
                sourceUrl = "https://sarkariresult.com.cm/admission/",
                category = ContentCategory.ADMISSION,
                enabled = true,
                priority = 5,
                sourceType = SourceType.WEB_SCRAPER,
                language = "Hindi",
                description = "Central & State university admission forms, counselling, and entrance exams.",
                checkIntervalMinutes = 180L,
                status = SourceStatus.ACTIVE
            ),
            ContentSourceConfig(
                sourceId = SOURCE_ID_WEEKLY_CA_PDF,
                sourceName = "GK Now — Weekly Current Affairs Hindi PDF",
                sourceUrl = "https://gknow.in/hi/weekly-current-affairs-pdf-in-hindi/",
                category = ContentCategory.CURRENT_AFFAIRS_PDF,
                enabled = true,
                priority = 6,
                sourceType = SourceType.PDF_DIRECTORY,
                language = "Hindi",
                description = "Date-wise and week-wise Hindi Current Affairs PDF repository.",
                checkIntervalMinutes = 180L,
                status = SourceStatus.ACTIVE
            ),
            ContentSourceConfig(
                sourceId = SOURCE_ID_WHATSAPP_REF,
                sourceName = "StudyMate WhatsApp Channel Reference",
                sourceUrl = "https://whatsapp.com/channel/0029VaAbQf01NCrYADMLt00L",
                category = ContentCategory.EXTERNAL_SOURCE,
                enabled = false, // Reference only as mandated by security rules
                priority = 7,
                sourceType = SourceType.REFERENCE_ONLY,
                language = "Hinglish",
                description = "Configured reference channel only. Not scraped directly.",
                checkIntervalMinutes = 1440L,
                status = SourceStatus.DISABLED
            )
        )
    }

    private val sourceMap = ConcurrentHashMap<String, ContentSourceConfig>().apply {
        DEFAULT_SOURCES.forEach { put(it.sourceId, it) }
    }

    private val _sourcesState = MutableStateFlow<List<ContentSourceConfig>>(getAllSources())
    val sourcesState: StateFlow<List<ContentSourceConfig>> = _sourcesState.asStateFlow()

    fun getAllSources(): List<ContentSourceConfig> {
        return sourceMap.values.sortedBy { it.priority }
    }

    fun getEnabledSources(): List<ContentSourceConfig> {
        return sourceMap.values
            .filter { it.enabled && it.sourceType != SourceType.REFERENCE_ONLY }
            .sortedBy { it.priority }
    }

    fun getSource(sourceId: String): ContentSourceConfig? {
        return sourceMap[sourceId]
    }

    fun toggleSourceEnabled(sourceId: String, enabled: Boolean) {
        val current = sourceMap[sourceId] ?: return
        val newStatus = if (!enabled) SourceStatus.DISABLED else {
            if (current.consecutiveFailures >= UNHEALTHY_FAILURE_THRESHOLD) SourceStatus.UNHEALTHY else SourceStatus.ACTIVE
        }
        val updated = current.copy(enabled = enabled, status = newStatus)
        sourceMap[sourceId] = updated
        _sourcesState.value = getAllSources()
    }

    fun updateSourcePriority(sourceId: String, newPriority: Int) {
        val current = sourceMap[sourceId] ?: return
        val updated = current.copy(priority = newPriority)
        sourceMap[sourceId] = updated
        _sourcesState.value = getAllSources()
    }

    fun updateCheckInterval(sourceId: String, intervalMinutes: Long) {
        val current = sourceMap[sourceId] ?: return
        val updated = current.copy(checkIntervalMinutes = intervalMinutes.coerceAtLeast(30L))
        sourceMap[sourceId] = updated
        _sourcesState.value = getAllSources()
    }

    fun recordSourceCheckResult(
        sourceId: String,
        isSuccess: Boolean,
        discoveredCount: Int = 0,
        errorMessage: String? = null
    ) {
        val current = sourceMap[sourceId] ?: return
        val now = System.currentTimeMillis()
        val newConsecutiveFailures = if (isSuccess) 0 else current.consecutiveFailures + 1
        val newStatus = when {
            !current.enabled -> SourceStatus.DISABLED
            newConsecutiveFailures >= UNHEALTHY_FAILURE_THRESHOLD -> SourceStatus.UNHEALTHY
            else -> SourceStatus.ACTIVE
        }

        val updated = current.copy(
            lastCheckedAt = now,
            lastSuccessAt = if (isSuccess) now else current.lastSuccessAt,
            lastError = if (isSuccess) null else errorMessage,
            consecutiveFailures = newConsecutiveFailures,
            status = newStatus,
            itemsDiscoveredCount = if (isSuccess) current.itemsDiscoveredCount + discoveredCount else current.itemsDiscoveredCount
        )
        sourceMap[sourceId] = updated
        _sourcesState.value = getAllSources()
    }

    fun resetSourceHealth(sourceId: String) {
        val current = sourceMap[sourceId] ?: return
        val updated = current.copy(
            consecutiveFailures = 0,
            status = if (current.enabled) SourceStatus.ACTIVE else SourceStatus.DISABLED,
            lastError = null
        )
        sourceMap[sourceId] = updated
        _sourcesState.value = getAllSources()
    }

    fun addCustomSource(config: ContentSourceConfig) {
        sourceMap[config.sourceId] = config
        _sourcesState.value = getAllSources()
    }
}
