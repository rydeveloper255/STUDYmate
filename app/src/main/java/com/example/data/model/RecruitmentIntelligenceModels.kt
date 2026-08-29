package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

enum class RecruitmentContentType(val label: String, val hindiLabel: String, val icon: String) {
    VACANCY("Latest Vacancy", "नवीनतम रिक्तियां / सरकारी नौकरी", "🚀"),
    RESULT("Results", "परीक्षा परिणाम / रिजल्ट", "🏆"),
    ADMIT_CARD("Admit Card", "प्रवेश पत्र / एडमिट कार्ड", "🎫"),
    ANSWER_KEY("Answer Key", "उत्तर कुंजी / आंसर की", "🔑"),
    NOTIFICATION("Notification", "आधिकारिक अधिसूचना", "📢"),
    EXAM_UPDATE("Exam Update", "परीक्षा तिथि / सिटी अपडेट", "🗓️"),
    OTHER_RECRUITMENT_UPDATE("Update", "भर्ती सूचना", "ℹ️")
}

enum class VacancyStatus(val label: String, val badgeColorHex: String, val isApplyActive: Boolean) {
    COMING_SOON("Coming Soon", "#3B82F6", false),
    OPEN("Open", "#10B981", true),
    LAST_DAY("Last Day to Apply", "#EF4444", true),
    CLOSED("Application Closed", "#6B7280", false),
    EXTENDED("Deadline Extended", "#8B5CF6", true),
    CANCELLED("Cancelled", "#DC2626", false),
    UNKNOWN("Verification Required", "#F59E0B", false)
}

enum class DeadlinePriority(val label: String, val badgeText: String, val colorHex: String) {
    URGENT("🔴 Urgent", "Last Day / 1 Day Left", "#EF4444"),
    CLOSING_SOON("🟠 Closing Soon", "2–3 Days Left", "#F97316"),
    APPLY_SOON("🟡 Apply Soon", "4–7 Days Left", "#EAB308"),
    OPEN("🟢 Open", "8+ Days Left", "#10B981"),
    CLOSED("⚪ Closed", "Application Closed", "#6B7280")
}

enum class RelevanceTier(val label: String, val colorHex: String) {
    HIGHLY_RELEVANT("Highly Relevant", "#10B981"),
    RELEVANT("Relevant", "#3B82F6"),
    MAYBE_RELEVANT("Maybe Relevant", "#F59E0B"),
    GENERAL("General Opportunity", "#6B7280")
}

enum class EligibilityMatchStatus(val label: String, val badgeText: String, val colorHex: String) {
    ELIGIBLE("Eligible", "Eligible based on your profile", "#10B981"),
    LIKELY_ELIGIBLE("Likely Eligible", "Likely eligible (Check relaxation)", "#3B82F6"),
    CHECK_REQUIRED("Eligibility Check Required", "Check your eligibility in official notice", "#F59E0B"),
    NOT_ELIGIBLE("Not Eligible", "Criteria does not match profile", "#EF4444")
}

enum class UserApplicationStatus(val label: String, val icon: String, val colorHex: String) {
    NONE("Not Applied", "⚪", "#6B7280"),
    INTERESTED("Interested", "⭐", "#8B5CF6"),
    PLANNING_TO_APPLY("Planning to Apply", "⏳", "#F59E0B"),
    APPLIED("Application Submitted", "✅", "#10B981"),
    EXAM_SCHEDULED("Exam Scheduled", "🗓️", "#3B82F6"),
    RESULT_AWAITED("Result Awaited", "⏳", "#06B6D4"),
    SELECTED("Selected", "🎉", "#10B981"),
    NOT_SELECTED("Not Selected", "❌", "#EF4444"),
    WITHDRAWN("Withdrawn", "🚫", "#9CA3AF")
}

enum class DataQualityScore(val label: String, val colorHex: String) {
    VERIFIED("Verified Official Source", "#10B981"),
    PARTIALLY_VERIFIED("Partially Verified", "#F59E0B"),
    NEEDS_REVIEW("Needs Verification", "#EF4444")
}

enum class VerificationConfidence(val label: String) {
    HIGH("High Confidence"),
    MEDIUM("Medium Confidence"),
    LOW("Needs Verification")
}

enum class RecruitmentCategory(val label: String, val icon: String) {
    ALL("All Sectors", "🌐"),
    RAILWAY("Railway (RRB/RRC)", "🚆"),
    SSC("SSC (CGL/CHSL/MTS/GD)", "🏛️"),
    BANKING("Banking (IBPS/SBI/RBI)", "🏦"),
    DEFENCE("Defence (NDA/CDS/Army/AF)", "🛡️"),
    UPSC("UPSC (Civil Services/NDA)", "⭐"),
    STATE_PSC("State PSC & Police", "👮"),
    TEACHING("Teaching (TET/CTET/KVS)", "📚"),
    ENGINEERING("Engineering & PSU (GATE/ISRO)", "⚙️"),
    MEDICAL("Medical & Nursing (NEET/AIIMS)", "🩺"),
    OTHER("Other Government Jobs", "💼")
}

enum class RecruitmentSortOption(val label: String) {
    RECOMMENDED("Recommended"),
    LAST_DATE_SOON("Deadline Soon"),
    NEWEST("Newest"),
    RECENTLY_UPDATED("Recently Updated"),
    EXAM_RELATED("Exam Related"),
    STATE_RELATED("State Related")
}

enum class RecruitmentLifecycleStage(val label: String, val hindiLabel: String, val badgeColorHex: String) {
    DISCOVERED("Discovered", "खोज की गई", "#6B7280"),
    PROCESSING("Processing", "प्रोसेसिंग", "#3B82F6"),
    VERIFYING("Verifying Notice", "सत्यापन जारी", "#F59E0B"),
    ACTIVE("Active / Applications Open", "आवेदन चालू", "#10B981"),
    APPLICATION_CLOSED("Application Closed", "आवेदन समाप्त", "#6B7280"),
    EXAM_UPCOMING("Exam Upcoming", "परीक्षा निकट", "#8B5CF6"),
    ADMIT_CARD_RELEASED("Admit Card Released", "एडमिट कार्ड जारी", "#06B6D4"),
    EXAM_COMPLETED("Exam Completed", "परीक्षा संपन्न", "#64748B"),
    ANSWER_KEY_RELEASED("Answer Key Released", "आंसर की जारी", "#F59E0B"),
    RESULT_RELEASED("Result Declared", "रिजल्ट घोषित", "#10B981"),
    FINAL_RESULT("Final Merit List", "अंतिम चयन सूची", "#059669"),
    ARCHIVED("Archived", "अभिलेखागार", "#475569")
}

enum class RecruitmentEventType(val label: String, val icon: String) {
    RECRUITMENT_CREATED("New Vacancy Verified", "🚀"),
    DEADLINE_EXTENDED("Deadline Extended", "🎉"),
    DEADLINE_APPROACHING("Application Closing Soon", "⏰"),
    DEADLINE_TODAY("Last Day to Apply", "🔴"),
    ADMIT_CARD_RELEASED("Admit Card Released", "🎫"),
    ANSWER_KEY_RELEASED("Answer Key Released", "📄"),
    RESULT_RELEASED("Result Declared", "📝"),
    EXAM_DATE_CHANGED("Exam Date Updated", "⚠️"),
    IMPORTANT_NOTICE("Important Official Update", "📢")
}

enum class NotificationPriority(val label: String) {
    CRITICAL("Critical Alert"),
    HIGH("High Priority"),
    NORMAL("Standard")
}

enum class NotificationDeliveryStatus(val label: String) {
    QUEUED("Queued"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

enum class DigestMode(val label: String, val description: String) {
    INSTANT("Instant Alerts", "Receive immediate notifications as verified events occur"),
    DAILY_DIGEST("Daily Morning Digest", "Grouped morning summary at 8:00 AM"),
    WEEKLY_DIGEST("Weekly Summary", "Comprehensive weekly wrap-up on Sunday")
}

data class RecruitmentNotificationSettings(
    val recruitmentNotificationsEnabled: Boolean = true,
    val newVacanciesEnabled: Boolean = true,
    val resultsEnabled: Boolean = true,
    val admitCardsEnabled: Boolean = true,
    val deadlineAlertsEnabled: Boolean = true,
    val examChangesEnabled: Boolean = true,
    val savedUpdatesEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val digestMode: String = DigestMode.INSTANT.name,
    val mutedRecruitmentIds: List<String> = emptyList(),
    val mutedCategories: List<String> = emptyList(),
    val targetExamOnly: Boolean = false,
    val homeStateOnly: Boolean = false
)

data class RecruitmentOutboxItem(
    val id: String = UUID.randomUUID().toString(),
    val notificationEventId: String, // deduplication key: e.g. "notif_rec_rrb_alp_2026_DEADLINE_EXTENDED_v2"
    val recruitmentId: String,
    val eventType: String,
    val eventVersion: String = "v1",
    val titleEn: String,
    val titleHi: String,
    val messageEn: String,
    val messageHi: String,
    val priority: String = NotificationPriority.NORMAL.name,
    val deepLink: String, // "recruitment://vacancy/{id}"
    val status: String = NotificationDeliveryStatus.SENT.name,
    val isRead: Boolean = false,
    val actionText: String = "View Details",
    val targetExamCategory: String = "",
    val targetState: String = "All India",
    val createdAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = System.currentTimeMillis(),
    val readAt: Long? = null
)

data class DailyRecruitmentDigest(
    val dateStr: String = "",
    val greeting: String = "Good morning!",
    val newMatchesCount: Int = 0,
    val closingSoonCount: Int = 0,
    val resultsCount: Int = 0,
    val admitCardsCount: Int = 0,
    val summaryEn: String = "",
    val summaryHi: String = "",
    val topOpportunityIds: List<String> = emptyList()
)

data class SourceHealthStatus(
    val name: String,
    val url: String,
    val isOnline: Boolean = true,
    val httpStatus: Int = 200,
    val latencyMs: Long = 180,
    val lastChecked: Long = System.currentTimeMillis(),
    val contentHash: String = "a1b2c3d4",
    val failureCount: Int = 0
)

data class AdminRecruitmentDiagnostics(
    val sourceHealthList: List<SourceHealthStatus> = emptyList(),
    val serperRequests: Int = 36,
    val serperSuccess: Int = 36,
    val serperLatencyMs: Long = 260,
    val geminiRequests: Int = 22,
    val geminiSuccess: Int = 22,
    val geminiLatencyMs: Long = 410,
    val verificationQueueCount: Int = 0,
    val conflictingDataCount: Int = 0,
    val failedExtractionsCount: Int = 0,
    val duplicateCandidatesResolved: Int = 14,
    val outboxTotalGenerated: Int = 18,
    val outboxDeliveredCount: Int = 18,
    val outboxSuppressedCount: Int = 3,
    val lastIngestionTime: Long = System.currentTimeMillis()
)

enum class NovaRecruitmentActionType(val actionCode: String, val description: String) {
    OPEN_VACANCIES("OPEN_VACANCIES", "Open Latest Vacancies feed"),
    OPEN_RESULTS("OPEN_RESULTS", "Open Results tab"),
    OPEN_ADMIT_CARDS("OPEN_ADMIT_CARDS", "Open Admit Cards tab"),
    OPEN_SAVED_RECRUITMENTS("OPEN_SAVED_RECRUITMENTS", "Open Saved Watchlist"),
    OPEN_RECRUITMENT("OPEN_RECRUITMENT", "Open specific recruitment details"),
    ENABLE_DEADLINE_ALERTS("ENABLE_DEADLINE_ALERTS", "Enable closing-soon deadline alerts"),
    OPEN_NOTIFICATION_SETTINGS("OPEN_NOTIFICATION_SETTINGS", "Open notification configuration")
}

data class RecruitmentTimelineEvent(
    val stage: String, // e.g. "Notification", "Application Start", "Application Last Date", "Correction Window", "Exam Date", "Admit Card", "Answer Key", "Result", "Final Merit List"
    val date: String,
    val status: String = "ACTIVE", // "UPCOMING", "ACTIVE", "COMPLETED", "EXTENDED", "RESCHEDULED"
    val details: String = "",
    val previousDate: String? = null,
    val isChanged: Boolean = false,
    val source: String? = null
)

data class UserRecruitmentProfile(
    val selectedExam: String = "Railway",
    val state: String = "All India",
    val preferredJobCategories: List<String> = listOf(RecruitmentCategory.RAILWAY.name, RecruitmentCategory.SSC.name),
    val educationQualification: String = "Graduation / Any Degree",
    val age: Int? = null,
    val birthYear: Int? = null,
    val preferredLanguage: String = "EN", // "EN" or "HI"
    val preferredDepartments: List<String> = emptyList(),
    val preferredSalaryRange: String = "Any",
    val notificationPreference: String = "SAVED_ONLY", // "ALL", "SAVED_ONLY", "DEADLINES_ONLY", "RESULTS_ADMIT_CARDS", "OFF"
    val watchlistCategories: List<String> = listOf(RecruitmentCategory.RAILWAY.name, RecruitmentCategory.SSC.name)
)

data class RecruitmentUserReport(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val reportCategory: String, // "Wrong deadline", "Wrong eligibility", "Wrong vacancy count", "Wrong result", "Wrong admit-card status", "Duplicate", "Outdated", "Other"
    val userComment: String = "",
    val reportedAt: Long = System.currentTimeMillis()
)

data class RecruitmentChangeSummary(
    val title: String,
    val changeType: String, // "DEADLINE_EXTENDED", "EXAM_DATE_CHANGED", "ADMIT_CARD_RELEASED", "RESULT_DECLARED", "VACANCY_CORRECTION", "CORRECTION_WINDOW"
    val summaryEn: String,
    val summaryHi: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Official Notification"
)

@Entity(tableName = "recruitment_items")
data class RecruitmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val organization: String,
    val postName: String,
    val examCategory: String = RecruitmentCategory.OTHER.name,
    val state: String = "All India",
    val contentType: String = RecruitmentContentType.VACANCY.name,
    val rawStatus: String = VacancyStatus.OPEN.name,
    val totalVacancies: Int? = null,
    val applicationStartDate: String? = null,   // "YYYY-MM-DD"
    val applicationLastDate: String? = null,    // "YYYY-MM-DD"
    val previousLastDate: String? = null,       // In case of extension
    val correctionDate: String? = null,
    val examDate: String? = null,
    val admitCardDate: String? = null,
    val resultDate: String? = null,
    val feeDetails: String = "Not specified",
    val salary: String = "Not specified",
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val ageRelaxation: String = "Not specified",
    val educationalQualification: String = "Not specified",
    val experienceRequired: String = "Not specified",
    val selectionProcess: List<String> = emptyList(),
    val documentsRequired: List<String> = emptyList(),
    val sourceUrl: String = "https://www.sarkariresult.com/",
    val officialSourceUrl: String = "",
    val applicationUrl: String = "",
    val officialPdfUrl: String = "",
    val summaryEn: String = "",
    val summaryHi: String = "",
    val whatShouldIDo: List<String> = emptyList(),
    val isVerified: Boolean = true,
    val verificationConfidence: String = VerificationConfidence.HIGH.name,
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val fetchedAt: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val hasDeadlineReminder: Boolean = false,
    val reminderDaysBefore: Int = 3,
    val resultType: String? = null,             // CBT Result, Scorecard, Merit List, Final Result, Answer Key
    val admitCardStatus: String? = null,        // Released, City Slip Out, Expected
    val isCorrectionNotice: Boolean = false,
    val isDeadlineExtended: Boolean = false,
    val correctionDetails: String = "",
    val categoryWiseVacanciesJson: String = "{}", // e.g. {"UR": 8000, "OBC": 4000, "SC": 2500, "ST": 1500, "EWS": 1500}
    // STEP 40 SMART RECRUITMENT INTELLIGENCE 2.0 EXTENSIONS
    val applicationStatus: String = UserApplicationStatus.NONE.name,
    val userApplicationNumber: String = "",     // Local-only, private
    val userRollNumber: String = "",            // Local-only, private
    val userAppliedPost: String = "",
    val userNotes: String = "",
    val documentsReadyList: List<String> = emptyList(),
    val checklistCheckedList: List<String> = emptyList(),
    val changeSummary: String = "",
    val importantUpdateBadge: String = "",      // "Updated Today", "Deadline Extended", "Exam Date Changed", "Admit Card Released"
    val whyRecommended: String = "",
    val eligibilityMatchStatus: String = EligibilityMatchStatus.CHECK_REQUIRED.name,
    val eligibilityExplanation: String = "",
    val personalRelevanceScore: Int = 50,
    val relevanceTier: String = RelevanceTier.RELEVANT.name,
    val dataQualityScore: String = DataQualityScore.VERIFIED.name,
    val contentHash: String = ""
) {
    /**
     * Strictly computes deterministic real-time status according to current date in Asia/Kolkata timezone.
     */
    fun getComputedStatus(): VacancyStatus {
        if (contentType != RecruitmentContentType.VACANCY.name) {
            return VacancyStatus.OPEN
        }
        if (rawStatus == VacancyStatus.CANCELLED.name) {
            return VacancyStatus.CANCELLED
        }
        return RecruitmentDateLogic.evaluateStatus(
            startDateStr = applicationStartDate,
            lastDateStr = applicationLastDate,
            isExtended = isDeadlineExtended,
            isCancelled = rawStatus == VacancyStatus.CANCELLED.name
        )
    }

    /**
     * Calculates deadline priority with label and color.
     */
    fun getDeadlinePriority(): DeadlinePriority {
        val days = RecruitmentDateLogic.calculateDaysRemaining(applicationLastDate) ?: return DeadlinePriority.OPEN
        return when {
            days < 0 -> DeadlinePriority.CLOSED
            days in 0..1 -> DeadlinePriority.URGENT
            days in 2..3 -> DeadlinePriority.CLOSING_SOON
            days in 4..7 -> DeadlinePriority.APPLY_SOON
            else -> DeadlinePriority.OPEN
        }
    }

    fun isClosingSoon(): Boolean {
        val days = RecruitmentDateLogic.calculateDaysRemaining(applicationLastDate)
        return days != null && days in 1..3
    }

    fun isClosingToday(): Boolean {
        val days = RecruitmentDateLogic.calculateDaysRemaining(applicationLastDate)
        return days == 0
    }

    fun isApplied(): Boolean {
        return applicationStatus == UserApplicationStatus.APPLIED.name ||
               applicationStatus == UserApplicationStatus.EXAM_SCHEDULED.name ||
               applicationStatus == UserApplicationStatus.RESULT_AWAITED.name ||
               applicationStatus == UserApplicationStatus.SELECTED.name
    }

    fun getRelevanceTierEnum(): RelevanceTier {
        return try {
            RelevanceTier.valueOf(relevanceTier)
        } catch (e: Exception) {
            RelevanceTier.RELEVANT
        }
    }

    fun getEligibilityStatusEnum(): EligibilityMatchStatus {
        return try {
            EligibilityMatchStatus.valueOf(eligibilityMatchStatus)
        } catch (e: Exception) {
            EligibilityMatchStatus.CHECK_REQUIRED
        }
    }

    fun getApplicationStatusEnum(): UserApplicationStatus {
        return try {
            UserApplicationStatus.valueOf(applicationStatus)
        } catch (e: Exception) {
            UserApplicationStatus.NONE
        }
    }
}

object RecruitmentDateLogic {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    fun getTodayKolkataString(): String {
        return sdf.format(Date())
    }

    fun evaluateStatus(
        startDateStr: String?,
        lastDateStr: String?,
        isExtended: Boolean = false,
        isCancelled: Boolean = false
    ): VacancyStatus {
        if (isCancelled) return VacancyStatus.CANCELLED

        val todayStr = getTodayKolkataString()

        if (!startDateStr.isNullOrBlank() && todayStr < startDateStr) {
            return VacancyStatus.COMING_SOON
        }

        if (lastDateStr.isNullOrBlank()) {
            return if (isExtended) VacancyStatus.EXTENDED else VacancyStatus.OPEN
        }

        return when {
            todayStr > lastDateStr -> VacancyStatus.CLOSED
            todayStr == lastDateStr -> VacancyStatus.LAST_DAY
            isExtended -> VacancyStatus.EXTENDED
            else -> VacancyStatus.OPEN
        }
    }

    fun calculateDaysRemaining(lastDateStr: String?): Int? {
        if (lastDateStr.isNullOrBlank()) return null
        return try {
            val today = sdf.parse(getTodayKolkataString()) ?: return null
            val target = sdf.parse(lastDateStr) ?: return null
            val diffMs = target.time - today.time
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            if (diffDays < 0) -1 else diffDays
        } catch (e: Exception) {
            null
        }
    }

    fun formatDeadlineDisplay(lastDateStr: String?): String {
        if (lastDateStr.isNullOrBlank()) return "Deadline: Not specified"
        val days = calculateDaysRemaining(lastDateStr)
        return when {
            days == null -> "Last Date: $lastDateStr"
            days < 0 -> "Application Closed ($lastDateStr)"
            days == 0 -> "🔴 Last Day to Apply!"
            days == 1 -> "🔴 1 Day Left (Closes Tomorrow)"
            days in 2..3 -> "🟠 $days Days Left (Closing Soon)"
            days in 4..7 -> "🟡 $days Days Left"
            else -> "🟢 Open ($days days left)"
        }
    }
}

data class RecruitmentFeedState(
    val selectedCategory: String = RecruitmentCategory.ALL.name,
    val selectedState: String = "All India",
    val selectedTab: String = RecruitmentContentType.VACANCY.name,
    val searchQuery: String = "",
    val sortOption: RecruitmentSortOption = RecruitmentSortOption.RECOMMENDED,
    val latestForYouVacancies: List<RecruitmentEntity> = emptyList(),
    val otherStateVacancies: List<RecruitmentEntity> = emptyList(),
    val allActiveVacancies: List<RecruitmentEntity> = emptyList(),
    val resultsList: List<RecruitmentEntity> = emptyList(),
    val admitCardsList: List<RecruitmentEntity> = emptyList(),
    val notificationsList: List<RecruitmentEntity> = emptyList(),
    val savedItems: List<RecruitmentEntity> = emptyList(),
    val activeTrackedApplications: List<RecruitmentEntity> = emptyList(),
    val userProfile: UserRecruitmentProfile = UserRecruitmentProfile(),
    val isLoading: Boolean = false,
    val lastSyncMillis: Long = System.currentTimeMillis(),
    val statusMessage: String = "✓ Up to date with official sources",
    val userTargetExam: String = "Railway",
    val userTargetState: String = "All India",
    val watchlistUpdatesCount: Int = 0,
    val findJobsMatches: List<RecruitmentEntity> = emptyList()
)
