package com.example.data.model

import java.util.UUID

/**
 * Step 55: Smart Notification & Personalization Engine 2.0 Models
 */

enum class SmartNotificationType(
    val category: NotificationCategory,
    val defaultPriority: NotificationPriority,
    val defaultExpiryMinutes: Long,
    val cooldownMinutes: Long
) {
    STUDY_REMINDER(NotificationCategory.STUDY, NotificationPriority.NORMAL, 45L, 15L),
    FOCUS_STARTED(NotificationCategory.STUDY, NotificationPriority.NORMAL, 30L, 5L),
    FOCUS_COMPLETED(NotificationCategory.STUDY, NotificationPriority.NORMAL, 60L, 5L),
    FOCUS_INTERRUPTED(NotificationCategory.STUDY, NotificationPriority.NORMAL, 30L, 5L),
    SCHEDULE_REMINDER(NotificationCategory.STUDY, NotificationPriority.NORMAL, 45L, 15L),
    MISSED_STUDY_SESSION(NotificationCategory.STUDY, NotificationPriority.NORMAL, 120L, 60L),
    GOAL_PROGRESS(NotificationCategory.STUDY, NotificationPriority.NORMAL, 180L, 60L),
    STREAK(NotificationCategory.STUDY, NotificationPriority.NORMAL, 360L, 120L),
    MOTIVATION(NotificationCategory.NOVA, NotificationPriority.NORMAL, 120L, 120L),
    NEW_VACANCY(NotificationCategory.EXAM_UPDATES, NotificationPriority.NORMAL, 7 * 24 * 60L, 60L),
    DEADLINE_SOON(NotificationCategory.EXAM_UPDATES, NotificationPriority.CRITICAL, 3 * 24 * 60L, 24 * 60L),
    DEADLINE_EXTENDED(NotificationCategory.EXAM_UPDATES, NotificationPriority.CRITICAL, 7 * 24 * 60L, 60L),
    RESULT_RELEASED(NotificationCategory.EXAM_UPDATES, NotificationPriority.CRITICAL, 14 * 24 * 60L, 60L),
    ADMIT_CARD_RELEASED(NotificationCategory.EXAM_UPDATES, NotificationPriority.CRITICAL, 7 * 24 * 60L, 60L),
    CURRENT_AFFAIRS(NotificationCategory.CURRENT_AFFAIRS, NotificationPriority.NORMAL, 12 * 60L, 180L),
    IMPORTANT_UPDATE(NotificationCategory.EXAM_UPDATES, NotificationPriority.CRITICAL, 7 * 24 * 60L, 60L),
    SYSTEM(NotificationCategory.SYSTEM, NotificationPriority.CRITICAL, 24 * 60L, 0L)
}

enum class DeliveryStatus {
    CREATED,
    RELEVANCE_FILTERED,
    SUPPRESSED_PREFERENCE,
    SUPPRESSED_QUIET_HOURS,
    SUPPRESSED_COOLDOWN,
    SUPPRESSED_ACTIVE_SESSION,
    DEDUPLICATED,
    QUEUED,
    SENT,
    EXPIRED,
    FAILED
}

data class SmartNotificationEvent(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String, // e.g. "notif_vacancy_rrb_alp_2026_v1"
    val userId: String = "local_user",
    val type: SmartNotificationType,
    val priority: NotificationPriority = type.defaultPriority,
    val title: String,
    val message: String,
    val hindiTitle: String? = null,
    val hindiMessage: String? = null,
    val deepLink: String = "HOME",
    val actionType: NovaActionType = NovaActionType.NONE,
    val actionPayload: String? = null,
    val actionText: String = "Open",
    val targetExamCategory: String? = null,
    val targetState: String? = null,
    val eventVersion: String = "v1",
    val relevanceScore: Int = 50,
    val isApplyActive: Boolean = false,
    val verifiedDeadlineDate: String? = null,
    val verifiedExamDate: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (type.defaultExpiryMinutes * 60 * 1000L)
) {
    val stableKey: String
        get() = "${userId}_${eventId}_${eventVersion}_${type.name}"
}

data class NotificationDeliveryLog(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val stableKey: String,
    val type: SmartNotificationType,
    val status: DeliveryStatus,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationBundle(
    val id: String = UUID.randomUUID().toString(),
    val category: NotificationCategory,
    val title: String,
    val message: String,
    val count: Int,
    val eventIds: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
