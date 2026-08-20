package com.example.notification

import android.content.Context
import android.util.Log
import com.example.data.model.NotificationPreference

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
        val engine = NovaNotificationEngine(context)

        // Respect Quiet Hours & Master Switch
        if (prefs != null && !prefs.masterEnabled) {
            Log.d(TAG, "Master notifications disabled, ignoring event: ${event.eventType}")
            return
        }

        if (engine.isQuietHours(prefs?.quietStartHour ?: 22, prefs?.quietEndHour ?: 7) && event.priority != EventPriority.HIGH) {
            Log.d(TAG, "Quiet hours active, skipping non-high priority event: ${event.eventType}")
            return
        }

        when (event.eventType) {
            ProactiveEventType.STUDY_SESSION_UPCOMING -> {
                if (prefs?.studyReminders != false) {
                    engine.sendStudyReminder(event.subject, event.topic, event.payloadMinutes)
                }
            }
            ProactiveEventType.STUDY_SESSION_COMPLETED,
            ProactiveEventType.STUDY_SESSION_STARTED -> {
                // SMART CANCELLATION: Cancel active reminders for this study session
                StudyNotificationManager.cancelAllReminders(context)
            }
            ProactiveEventType.STUDY_SESSION_MISSED -> {
                if (prefs?.missedStudyReminders != false) {
                    engine.sendMissedSessionRecovery(event.subject, event.topic, event.payloadMinutes)
                }
            }
            ProactiveEventType.REVISION_DUE,
            ProactiveEventType.REVISION_OVERDUE -> {
                engine.sendRevisionReminder(event.subject, event.topic)
            }
            ProactiveEventType.MOCK_RECOMMENDED -> {
                engine.sendMockTestReminder(event.subject, if (event.title.isNotBlank()) event.title else "Full Practice Test")
            }
            ProactiveEventType.PERFORMANCE_IMPROVED,
            ProactiveEventType.PERFORMANCE_NEEDS_ATTENTION -> {
                if (prefs?.motivationalQuotes != false) {
                    engine.sendPerformanceInsight(event.subject, event.message, event.eventType == ProactiveEventType.PERFORMANCE_IMPROVED)
                }
            }
            ProactiveEventType.EXAM_DATE_APPROACHING -> {
                if (prefs?.examCountdownAlerts != false) {
                    engine.sendExamCountdownAlert(event.subject, event.payloadMinutes)
                }
            }
            ProactiveEventType.DISTRACTION_THRESHOLD_REACHED -> {
                engine.sendSocialMediaNudge(event.title, event.payloadMinutes, if (event.subject.isNotBlank()) event.subject else "Study Session")
            }
            else -> {
                Log.d(TAG, "Event logged: ${event.eventType}")
            }
        }
    }
}
