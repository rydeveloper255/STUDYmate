package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.service.NovaVoiceManager
import com.example.service.voice.NovaVoiceEmotion
import java.util.Calendar

/**
 * Centralized NOVA Intelligent Notification Engine.
 * Features:
 * - Anti-spam cooldown checks
 * - Quiet hours enforcement
 * - Actionable buttons [Start Study], [Snooze], [Skip]
 * - Voice notification readout if enabled
 */
class NovaNotificationEngine(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences("nova_notification_engine_prefs", Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_NOVA_COACH = "nova_coach_channel"
        const val CHANNEL_NOVA_DAILY_BRIEF = "nova_daily_brief_channel"
        const val CHANNEL_NOVA_ALERTS = "nova_alerts_channel"

        const val ACTION_START_STUDY = "com.example.studymate.ACTION_START_STUDY_FROM_NOTIF"
        const val ACTION_SNOOZE_STUDY = "com.example.studymate.ACTION_SNOOZE_STUDY"
        const val ACTION_SKIP_STUDY = "com.example.studymate.ACTION_SKIP_STUDY"

        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_TOPIC = "extra_topic"
        const val EXTRA_MINUTES = "extra_minutes"

        private const val KEY_LAST_COACH_NOTIF_TIME = "last_coach_notif_time"
        private const val COOLDOWN_MILLIS = 35 * 60 * 1000L // 35 minutes minimum between coach nudges
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val coachChannel = NotificationChannel(
                CHANNEL_NOVA_COACH,
                "NOVA Study Coach & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Intelligent study reminders, missed-session recovery, and distraction nudges from NOVA"
                enableVibration(true)
            }

            val briefChannel = NotificationChannel(
                CHANNEL_NOVA_DAILY_BRIEF,
                "NOVA Daily Brief & Review",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning daily briefings and evening progress summaries"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_NOVA_ALERTS,
                "NOVA Exam & Achievement Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Exam countdowns, streak milestones, and goal celebrations"
            }

            notificationManager.createNotificationChannels(listOf(coachChannel, briefChannel, alertsChannel))
        }
    }

    fun isQuietHours(startHour: Int = 22, endHour: Int = 7): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) {
            currentHour >= startHour || currentHour < endHour
        } else {
            currentHour in startHour until endHour
        }
    }

    fun canSendCoachNotification(): Boolean {
        val lastTime = prefs.getLong(KEY_LAST_COACH_NOTIF_TIME, 0L)
        return (System.currentTimeMillis() - lastTime) >= COOLDOWN_MILLIS
    }

    private fun recordNotificationSent() {
        prefs.edit().putLong(KEY_LAST_COACH_NOTIF_TIME, System.currentTimeMillis()).apply()
    }

    fun sendStudyReminder(
        subject: String,
        topic: String,
        minutes: Int = 25,
        voiceReadoutEnabled: Boolean = false
    ) {
        if (isQuietHours() || !canSendCoachNotification()) return
        recordNotificationSent()

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SUBJECT, subject)
            putExtra(EXTRA_TOPIC, topic)
            putExtra(EXTRA_MINUTES, minutes)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Start Study
        val startIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_START_STUDY
            putExtra(EXTRA_SUBJECT, subject)
            putExtra(EXTRA_TOPIC, topic)
            putExtra(EXTRA_MINUTES, minutes)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 15m
        val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_STUDY
            putExtra(EXTRA_SUBJECT, subject)
            putExtra(EXTRA_TOPIC, topic)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            1003,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Skip
        val skipIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_SKIP_STUDY
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            1004,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Boss 📚 $subject ka time ho gaya"
        val message = if (topic.isNotBlank()) "Aaj $topic complete karna hai. $minutes minutes start karein?" else "$minutes minutes ka study session start karein?"

        val notification = NotificationCompat.Builder(context, CHANNEL_NOVA_COACH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nNOVA is ready to activate Focus Shield & app blocker."))
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_media_play, "Start Study", startPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze 15m", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip", skipPendingIntent)
            .build()

        notificationManager.notify(2001, notification)
    }

    fun sendMissedSessionRecovery(
        subject: String,
        topic: String,
        suggestedMinutes: Int = 20
    ) {
        if (isQuietHours()) return

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            2002,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_START_STUDY
            putExtra(EXTRA_SUBJECT, subject)
            putExtra(EXTRA_TOPIC, topic)
            putExtra(EXTRA_MINUTES, suggestedMinutes)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            2003,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "NOVA Smart Recovery 🔄"
        val message = "Boss, $subject session miss ho gaya. Ek quick ${suggestedMinutes}m recovery kar lete hain?"

        val notification = NotificationCompat.Builder(context, CHANNEL_NOVA_COACH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message Baaki schedule kal adjust kar denge."))
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_media_play, "Start Recovery", startPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip", null)
            .build()

        notificationManager.notify(2002, notification)
    }

    fun sendDailyBriefing(briefingText: String) {
        if (isQuietHours()) return

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NOVA_DAILY_BRIEF)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NOVA Daily Brief ☀️")
            .setContentText("Good morning Boss! Check today's study plan & goals.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(briefingText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(3001, notification)
    }

    fun sendDailyReview(reviewText: String) {
        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            3002,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NOVA_DAILY_BRIEF)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NOVA Daily Review 🌙")
            .setContentText("Check your study hours and progress summary for today.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(reviewText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(3002, notification)
    }

    fun sendSocialMediaNudge(appName: String, usageMins: Int, pendingSubject: String) {
        if (isQuietHours() || !canSendCoachNotification()) return
        recordNotificationSent()

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            4001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_START_STUDY
            putExtra(EXTRA_SUBJECT, pendingSubject)
            putExtra(EXTRA_TOPIC, "Focused Study")
            putExtra(EXTRA_MINUTES, 25)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            4002,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Boss 😅 $appName par $usageMins mins ho gaye"
        val message = "Aaj ka $pendingSubject session pending hai. 25 minutes padh lete hain?"

        val notification = NotificationCompat.Builder(context, CHANNEL_NOVA_COACH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_media_play, "Start 25m", startPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", null)
            .build()

        notificationManager.notify(4001, notification)
    }
}
