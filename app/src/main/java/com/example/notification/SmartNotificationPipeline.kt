package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DeliveryStatus
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationDeliveryLog
import com.example.data.model.NotificationPreference
import com.example.data.model.NotificationPriority
import com.example.data.model.SmartNotificationEvent
import com.example.data.model.SmartNotificationType
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Step 55: Smart Notification Pipeline 2.0
 *
 * Centralized, 11-stage pipeline for intelligent notification dispatching:
 * EVENT -> VALIDATION -> RELEVANCE -> ACTIVITY_AWARENESS -> EXPIRY -> QUIET_HOURS ->
 * COOLDOWN -> DEDUPLICATION -> BUNDLING -> SEND -> DELIVERY_LOG
 */
class SmartNotificationPipeline private constructor(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences("smart_notification_pipeline_prefs", Context.MODE_PRIVATE)

    private val _deliveryLogs = MutableStateFlow<List<NotificationDeliveryLog>>(emptyList())
    val deliveryLogs: StateFlow<List<NotificationDeliveryLog>> = _deliveryLogs.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<SmartNotificationEvent>>(emptyList())
    val recentEvents: StateFlow<List<SmartNotificationEvent>> = _recentEvents.asStateFlow()

    private val dispatchedFingerprints = mutableSetOf<String>()
    private val categoryCooldowns = mutableMapOf<String, Long>()
    private val lowPriorityQueue = mutableListOf<SmartNotificationEvent>()

    companion object {
        private const val TAG = "SmartNotifPipeline"

        const val CHANNEL_CRITICAL_UPDATES = "channel_critical_updates"
        const val CHANNEL_STUDY_ALERTS = "channel_study_alerts"
        const val CHANNEL_VACANCY_RADAR = "channel_vacancy_radar"
        const val CHANNEL_NOVA_MOTIVATION = "channel_nova_motivation"
        const val CHANNEL_GENERAL = "channel_general"

        private const val KEY_DISPATCHED_KEYS = "dispatched_keys_json"
        private const val KEY_LOGS_JSON = "delivery_logs_json"
        private const val KEY_RECENT_EVENTS_JSON = "recent_events_json"

        @Volatile
        private var INSTANCE: SmartNotificationPipeline? = null

        fun getInstance(context: Context): SmartNotificationPipeline {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartNotificationPipeline(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        createNotificationChannels()
        loadPersistedState()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL_UPDATES,
                "Important Updates & Results",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical exam results, admit cards, and application deadline alerts"
                enableVibration(true)
            }

            val studyChannel = NotificationChannel(
                CHANNEL_STUDY_ALERTS,
                "Study & Focus Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily study plan alerts, focus milestones, and spaced repetition"
                enableVibration(true)
            }

            val vacancyChannel = NotificationChannel(
                CHANNEL_VACANCY_RADAR,
                "Vacancy & Recruitment Radar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Personalized government job openings and verified notifications"
            }

            val motivationChannel = NotificationChannel(
                CHANNEL_NOVA_MOTIVATION,
                "Daily Motivation & Nova Coach",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Supportive Hinglish motivation and mindset boosts"
                setShowBadge(false)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General & System",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannels(
                listOf(criticalChannel, studyChannel, vacancyChannel, motivationChannel, generalChannel)
            )
        }
    }

    fun updatePreferences(prefs: NotificationPreference) {
        // No-op or cache local state if needed
        Log.d(TAG, "Notification preferences updated: masterEnabled=${prefs.masterEnabled}")
    }

    /**
     * Overload for simpler calls
     */
    fun processAndDispatchEvent(
        event: SmartNotificationEvent,
        notificationPrefs: NotificationPreference? = null
    ): DeliveryStatus {
        return processAndDispatchEvent(
            event = event,
            userProfile = null,
            notificationPrefs = notificationPrefs ?: NotificationPreference()
        )
    }

    /**
     * Central entry point: Process an incoming notification event through the 11-stage pipeline.
     */
    fun processAndDispatchEvent(
        event: SmartNotificationEvent,
        userProfile: UserProfile?,
        notificationPrefs: NotificationPreference,
        isUserInActiveFocus: Boolean = false,
        currentScreen: String = ""
    ): DeliveryStatus {
        val stableKey = event.stableKey

        // STAGE 1 & 2: VALIDATION
        if (event.title.isBlank() || event.message.isBlank()) {
            recordLog(event, DeliveryStatus.FAILED, "Title or message was blank")
            return DeliveryStatus.FAILED
        }

        // STAGE 3: USER PREFERENCE CHECK
        if (!notificationPrefs.masterEnabled) {
            recordLog(event, DeliveryStatus.SUPPRESSED_PREFERENCE, "Master notifications disabled by user")
            return DeliveryStatus.SUPPRESSED_PREFERENCE
        }

        val isCategoryEnabled = when (event.type) {
            SmartNotificationType.STUDY_REMINDER,
            SmartNotificationType.SCHEDULE_REMINDER -> notificationPrefs.studyReminders && notificationPrefs.scheduleReminders
            SmartNotificationType.FOCUS_STARTED -> notificationPrefs.focusStartedAlerts
            SmartNotificationType.FOCUS_COMPLETED -> notificationPrefs.focusCompletedAlerts
            SmartNotificationType.FOCUS_INTERRUPTED -> notificationPrefs.focusInterruptedAlerts
            SmartNotificationType.MISSED_STUDY_SESSION -> notificationPrefs.missedStudyReminders
            SmartNotificationType.GOAL_PROGRESS -> notificationPrefs.dailyGoalReminders
            SmartNotificationType.STREAK -> notificationPrefs.streakAlerts
            SmartNotificationType.MOTIVATION -> notificationPrefs.motivationalQuotes
            SmartNotificationType.NEW_VACANCY -> notificationPrefs.vacancyAlerts && notificationPrefs.examUpdatesReminders
            SmartNotificationType.DEADLINE_SOON,
            SmartNotificationType.DEADLINE_EXTENDED -> notificationPrefs.deadlineAlerts && notificationPrefs.examUpdatesReminders
            SmartNotificationType.RESULT_RELEASED -> notificationPrefs.resultAlerts && notificationPrefs.examUpdatesReminders
            SmartNotificationType.ADMIT_CARD_RELEASED -> notificationPrefs.admitCardAlerts && notificationPrefs.examUpdatesReminders
            SmartNotificationType.CURRENT_AFFAIRS -> notificationPrefs.currentAffairsReminders
            SmartNotificationType.IMPORTANT_UPDATE -> notificationPrefs.examUpdatesReminders
            SmartNotificationType.SYSTEM -> true
        }

        if (!isCategoryEnabled) {
            recordLog(event, DeliveryStatus.SUPPRESSED_PREFERENCE, "Category ${event.type.name} disabled in settings")
            return DeliveryStatus.SUPPRESSED_PREFERENCE
        }

        // Check muted categories for vacancy / exam updates
        event.targetExamCategory?.let { cat ->
            if (notificationPrefs.mutedCategories.any { it.equals(cat, ignoreCase = true) }) {
                recordLog(event, DeliveryStatus.SUPPRESSED_PREFERENCE, "Exam category $cat is muted")
                return DeliveryStatus.SUPPRESSED_PREFERENCE
            }
        }

        // STAGE 4: USER RELEVANCE CHECK
        if (event.type == SmartNotificationType.NEW_VACANCY) {
            if (event.relevanceScore < SmartRelevanceEngine.MINIMUM_RELEVANCE_THRESHOLD) {
                recordLog(event, DeliveryStatus.RELEVANCE_FILTERED, "Relevance score ${event.relevanceScore} below threshold")
                return DeliveryStatus.RELEVANCE_FILTERED
            }
        }

        // STAGE 5: USER ACTIVITY AWARENESS
        if (isUserInActiveFocus && event.priority != NotificationPriority.CRITICAL && event.type != SmartNotificationType.FOCUS_COMPLETED) {
            recordLog(event, DeliveryStatus.SUPPRESSED_ACTIVE_SESSION, "User currently in active focus mode")
            return DeliveryStatus.SUPPRESSED_ACTIVE_SESSION
        }

        if (currentScreen.isNotBlank() && event.deepLink.isNotBlank() && currentScreen.equals(event.deepLink, ignoreCase = true)) {
            recordLog(event, DeliveryStatus.SUPPRESSED_ACTIVE_SESSION, "User already viewing destination screen: $currentScreen")
            return DeliveryStatus.SUPPRESSED_ACTIVE_SESSION
        }

        // STAGE 6: EXPIRY CHECK
        if (System.currentTimeMillis() > event.expiresAt) {
            recordLog(event, DeliveryStatus.EXPIRED, "Event expired at ${event.expiresAt}")
            return DeliveryStatus.EXPIRED
        }

        // STAGE 7: QUIET HOURS CHECK
        if (isCurrentTimeInQuietHours(notificationPrefs)) {
            if (event.priority != NotificationPriority.CRITICAL) {
                recordLog(event, DeliveryStatus.SUPPRESSED_QUIET_HOURS, "Suppressed due to active quiet hours")
                return DeliveryStatus.SUPPRESSED_QUIET_HOURS
            }
        }

        // STAGE 8: RATE LIMITING & COOLDOWN
        val now = System.currentTimeMillis()
        val lastSent = categoryCooldowns[event.type.name] ?: 0L
        val cooldownMillis = event.type.cooldownMinutes * 60 * 1000L

        if (now - lastSent < cooldownMillis) {
            recordLog(event, DeliveryStatus.SUPPRESSED_COOLDOWN, "In cooldown window for ${event.type.name}")
            return DeliveryStatus.SUPPRESSED_COOLDOWN
        }

        // STAGE 9: DEDUPLICATION
        if (dispatchedFingerprints.contains(stableKey)) {
            recordLog(event, DeliveryStatus.DEDUPLICATED, "Event $stableKey already dispatched")
            return DeliveryStatus.DEDUPLICATED
        }

        // STAGE 10: BUNDLING FOR LOW-PRIORITY NOTIFICATIONS
        if (event.priority == NotificationPriority.NORMAL && event.type == SmartNotificationType.CURRENT_AFFAIRS) {
            lowPriorityQueue.add(event)
            if (lowPriorityQueue.size >= 3) {
                val bundleTitle = "📰 3 New Exam Updates Ready"
                val bundleMsg = "Latest daily current affairs and recruitment notices are ready in StudyMate."
                val bundledEvent = event.copy(
                    title = bundleTitle,
                    message = bundleMsg,
                    eventId = "bundle_${System.currentTimeMillis()}"
                )
                lowPriorityQueue.clear()
                return dispatchToSystem(bundledEvent)
            }
        }

        // STAGE 11: DISPATCH TO SYSTEM & LOG
        return dispatchToSystem(event)
    }

    private fun dispatchToSystem(event: SmartNotificationEvent): DeliveryStatus {
        try {
            val channelId = when (event.type) {
                SmartNotificationType.DEADLINE_SOON,
                SmartNotificationType.DEADLINE_EXTENDED,
                SmartNotificationType.RESULT_RELEASED,
                SmartNotificationType.ADMIT_CARD_RELEASED,
                SmartNotificationType.IMPORTANT_UPDATE,
                SmartNotificationType.SYSTEM -> CHANNEL_CRITICAL_UPDATES

                SmartNotificationType.STUDY_REMINDER,
                SmartNotificationType.FOCUS_STARTED,
                SmartNotificationType.FOCUS_COMPLETED,
                SmartNotificationType.FOCUS_INTERRUPTED,
                SmartNotificationType.SCHEDULE_REMINDER,
                SmartNotificationType.MISSED_STUDY_SESSION,
                SmartNotificationType.GOAL_PROGRESS,
                SmartNotificationType.STREAK -> CHANNEL_STUDY_ALERTS

                SmartNotificationType.NEW_VACANCY -> CHANNEL_VACANCY_RADAR
                SmartNotificationType.MOTIVATION -> CHANNEL_NOVA_MOTIVATION
                else -> CHANNEL_GENERAL
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_deep_link", event.deepLink)
                putExtra("extra_action_type", event.actionType.name)
                putExtra("extra_action_payload", event.actionPayload ?: "")
                putExtra("extra_event_id", event.eventId)
            }

            val requestCode = (event.eventId.hashCode() and 0x7FFFFFFF)
            val pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.title)
                .setContentText(event.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(event.message))
                .setPriority(
                    if (event.priority == NotificationPriority.CRITICAL) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (event.actionText.isNotBlank()) {
                builder.addAction(0, event.actionText, pendingIntent)
            }

            val notifId = (event.stableKey.hashCode() and 0x7FFFFFFF)
            NotificationManagerCompat.from(context).notify(notifId, builder.build())

            // Update in-memory state & cooldowns
            dispatchedFingerprints.add(event.stableKey)
            categoryCooldowns[event.type.name] = System.currentTimeMillis()

            // Update recent events for Nova contextual awareness
            val updatedRecent = (listOf(event) + _recentEvents.value).take(50)
            _recentEvents.value = updatedRecent

            recordLog(event, DeliveryStatus.SENT, "Dispatched successfully to channel: $channelId")
            savePersistedState()
            return DeliveryStatus.SENT
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch notification: ${e.message}", e)
            recordLog(event, DeliveryStatus.FAILED, "Exception: ${e.message}")
            return DeliveryStatus.FAILED
        }
    }

    private fun isCurrentTimeInQuietHours(prefs: NotificationPreference): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = prefs.quietStartHour * 60 + prefs.quietStartMinute
        val endMinutes = prefs.quietEndHour * 60 + prefs.quietEndMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    private fun recordLog(event: SmartNotificationEvent, status: DeliveryStatus, reason: String) {
        val entry = NotificationDeliveryLog(
            eventId = event.eventId,
            stableKey = event.stableKey,
            type = event.type,
            status = status,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        val updated = (listOf(entry) + _deliveryLogs.value).take(100)
        _deliveryLogs.value = updated
        saveLogsToDisk()
    }

    private fun savePersistedState() {
        try {
            val array = JSONArray()
            dispatchedFingerprints.toList().takeLast(200).forEach { array.put(it) }
            prefs.edit().putString(KEY_DISPATCHED_KEYS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving dispatched keys: ${e.message}")
        }
    }

    private fun saveLogsToDisk() {
        try {
            val array = JSONArray()
            _deliveryLogs.value.take(50).forEach { log ->
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("eventId", log.eventId)
                    put("stableKey", log.stableKey)
                    put("type", log.type.name)
                    put("status", log.status.name)
                    put("reason", log.reason)
                    put("timestamp", log.timestamp)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_LOGS_JSON, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving delivery logs: ${e.message}")
        }
    }

    private fun loadPersistedState() {
        try {
            val keysStr = prefs.getString(KEY_DISPATCHED_KEYS, null)
            if (!keysStr.isNullOrBlank()) {
                val array = JSONArray(keysStr)
                for (i in 0 until array.length()) {
                    dispatchedFingerprints.add(array.getString(i))
                }
            }

            val logsStr = prefs.getString(KEY_LOGS_JSON, null)
            if (!logsStr.isNullOrBlank()) {
                val array = JSONArray(logsStr)
                val list = mutableListOf<NotificationDeliveryLog>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        NotificationDeliveryLog(
                            id = obj.optString("id"),
                            eventId = obj.optString("eventId"),
                            stableKey = obj.optString("stableKey"),
                            type = try { SmartNotificationType.valueOf(obj.optString("type")) } catch (e: Exception) { SmartNotificationType.SYSTEM },
                            status = try { DeliveryStatus.valueOf(obj.optString("status")) } catch (e: Exception) { DeliveryStatus.SENT },
                            reason = obj.optString("reason"),
                            timestamp = obj.optLong("timestamp")
                        )
                    )
                }
                _deliveryLogs.value = list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading persisted state: ${e.message}")
        }
    }

    /**
     * Retrieve recent notification events to supply to Nova AI Brain context.
     */
    fun getRecentNotificationsForNova(): List<SmartNotificationEvent> {
        return _recentEvents.value.take(15)
    }

    /**
     * Clears in-memory test cache (useful for automated testing).
     */
    fun clearCacheForTesting() {
        dispatchedFingerprints.clear()
        categoryCooldowns.clear()
        _deliveryLogs.value = emptyList()
        _recentEvents.value = emptyList()
        lowPriorityQueue.clear()
    }
}
