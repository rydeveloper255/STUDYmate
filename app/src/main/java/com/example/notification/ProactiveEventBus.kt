package com.example.notification

import android.content.Context
import android.util.Log
import com.example.data.model.*

enum class ProactiveEventType {
    STUDY_SESSION_UPCOMING,
    STUDY_SESSION_STARTED,
    STUDY_SESSION_COMPLETED,
    STUDY_SESSION_MISSED,
    REVISION_DUE,
    REVISION_OVERDUE,
    MOCK_RECOMMENDED,
    MOCK_COMPLETED,
    PERFORMANCE_IMPROVED,
    PERFORMANCE_NEEDS_ATTENTION,
    EXAM_DATE_APPROACHING,
    PLAN_CHANGED,
    FOCUS_SESSION_STARTED,
    FOCUS_SESSION_COMPLETED,
    DISTRACTION_THRESHOLD_REACHED
}

enum class EventPriority {
    LOW, NORMAL, HIGH
}

data class ProactiveEvent(
    val eventType: ProactiveEventType,
    val subject: String = "",
    val topic: String = "",
    val examId: String = "",
    val title: String = "",
    val message: String = "",
    val priority: EventPriority = EventPriority.NORMAL,
    val payloadMinutes: Int = 20,
    val timestamp: Long = System.currentTimeMillis()
)

object ProactiveEventBus {
    private const val TAG = "ProactiveEventBus"

    fun emitEvent(
        context: Context,
        event: ProactiveEvent,
        prefs: NotificationPreference? = null
    ) {
        val pipeline = SmartNotificationPipeline.getInstance(context)

        // Handle cancellations on study start/complete
        if (event.eventType == ProactiveEventType.STUDY_SESSION_STARTED ||
            event.eventType == ProactiveEventType.STUDY_SESSION_COMPLETED
        ) {
            StudyNotificationManager.cancelAllReminders(context)
            Log.d(TAG, "Smart cancellation: cleared pending alarms for ${event.eventType}")
            return
        }

        // Map proactive event to SmartNotificationType & NotificationPriority
        val (notifType, notifPriority) = when (event.eventType) {
            ProactiveEventType.STUDY_SESSION_UPCOMING -> Pair(
                SmartNotificationType.STUDY_REMINDER,
                NotificationPriority.NORMAL
            )
            ProactiveEventType.STUDY_SESSION_MISSED -> Pair(
                SmartNotificationType.MISSED_STUDY_SESSION,
                NotificationPriority.NORMAL
            )
            ProactiveEventType.REVISION_DUE, ProactiveEventType.REVISION_OVERDUE -> Pair(
                SmartNotificationType.SCHEDULE_REMINDER,
                NotificationPriority.NORMAL
            )
            ProactiveEventType.MOCK_RECOMMENDED -> Pair(
                SmartNotificationType.SCHEDULE_REMINDER,
                NotificationPriority.NORMAL
            )
            ProactiveEventType.EXAM_DATE_APPROACHING -> Pair(
                SmartNotificationType.IMPORTANT_UPDATE,
                NotificationPriority.CRITICAL
            )
            ProactiveEventType.PERFORMANCE_IMPROVED, ProactiveEventType.PERFORMANCE_NEEDS_ATTENTION -> Pair(
                SmartNotificationType.MOTIVATION,
                NotificationPriority.NORMAL
            )
            ProactiveEventType.DISTRACTION_THRESHOLD_REACHED -> Pair(
                SmartNotificationType.FOCUS_INTERRUPTED,
                NotificationPriority.CRITICAL
            )
            else -> Pair(
                SmartNotificationType.SYSTEM,
                NotificationPriority.NORMAL
            )
        }

        val smartEvent = SmartNotificationEvent(
            eventId = "proactive_${event.eventType.name}_${event.timestamp}",
            type = notifType,
            priority = notifPriority,
            title = if (event.title.isNotBlank()) event.title else when (event.eventType) {
                ProactiveEventType.STUDY_SESSION_UPCOMING -> "📚 Study Time: ${event.subject}"
                ProactiveEventType.STUDY_SESSION_MISSED -> "💙 Missed Session Recovery"
                ProactiveEventType.REVISION_DUE -> "🧠 Revision Due: ${event.subject}"
                ProactiveEventType.MOCK_RECOMMENDED -> "✍️ Recommended Mock Test"
                ProactiveEventType.EXAM_DATE_APPROACHING -> "⏳ Exam Countdown Alert"
                ProactiveEventType.DISTRACTION_THRESHOLD_REACHED -> "🛡️ Focus Shield Reminder"
                else -> "🔔 StudyMate Notification"
            },
            message = if (event.message.isNotBlank()) event.message else when (event.eventType) {
                ProactiveEventType.STUDY_SESSION_UPCOMING -> "Aapka ${event.payloadMinutes}-minute session (${event.topic}) start hone wala hai. Ready ho?"
                ProactiveEventType.STUDY_SESSION_MISSED -> "Koi baat nahi! Ek short 15-20 min quick revision se track par wapas aa sakte ho."
                ProactiveEventType.REVISION_DUE -> "Spaced repetition reminder: ${event.topic} ko revise karke concept clear rakhein."
                ProactiveEventType.MOCK_RECOMMENDED -> "Ek full practice test solve karke apni weak areas test karein."
                ProactiveEventType.EXAM_DATE_APPROACHING -> "${event.payloadMinutes} din bache hain exam ke liye. Planned target follow karte rahein!"
                ProactiveEventType.DISTRACTION_THRESHOLD_REACHED -> "${event.title} screen time badh raha hai. Wapas session me focus karte hain!"
                else -> "StudyMate update available."
            },
            targetExamCategory = event.examId.ifBlank { null },
            timestamp = event.timestamp
        )

        pipeline.processAndDispatchEvent(smartEvent, prefs)
    }
}
