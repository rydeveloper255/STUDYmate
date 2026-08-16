package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import java.util.Calendar

data class AppNotificationSettings(
    val studyReminders: Boolean = true,
    val motivation: Boolean = true,
    val focusShield: Boolean = true,
    val progress: Boolean = true,
    val examAlerts: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStartHour: Int = 22, // 10:00 PM
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,   // 7:00 AM
    val quietHoursEndMinute: Int = 0,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 30,
    val dailyMotivationLimit: Int = 1,
    val reminderBehavior: String = "Gentle"
)

object StudyNotificationManager {
    const val CHANNEL_STUDY_REMINDERS = "study_reminders"
    const val CHANNEL_FOCUS_SHIELD = "focus_shield"
    const val CHANNEL_MOTIVATION = "motivation"
    const val CHANNEL_PROGRESS = "progress"
    const val CHANNEL_EXAM_ALERTS = "exam_alerts"

    const val ACTION_START_FOCUS = "com.example.studymate.ACTION_START_FOCUS"
    const val ACTION_SNOOZE_STUDY = "com.example.studymate.ACTION_SNOOZE_STUDY"

    private const val PREFS_NAME = "studymate_notifications_prefs"

    val MOTIVATIONAL_QUOTES = listOf(
        "💪 One focused session today can move you closer to your goal.",
        "📚 Small progress every day becomes a big result.",
        "🚀 Your future self will thank you for today's effort.",
        "🧠 Consistency beats intensity. Keep showing up!",
        "🌟 You don't have to be great to start, but you have to start to be great.",
        "🎯 Clear your mind, pick one topic, and give it 25 solid minutes.",
        "✨ Mastery isn't magic—it's just regular, focused active recall."
    )

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_STUDY_REMINDERS,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when scheduled study sessions begin and gentle catch-up reminders"
                },
                NotificationChannel(
                    CHANNEL_FOCUS_SHIELD,
                    "Focus Shield",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Active study session timers and completion updates"
                },
                NotificationChannel(
                    CHANNEL_MOTIVATION,
                    "Motivation",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily inspirational reminders and positive encouragement"
                },
                NotificationChannel(
                    CHANNEL_PROGRESS,
                    "Progress",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Weekly reports, streak achievements, and mastery milestones"
                },
                NotificationChannel(
                    CHANNEL_EXAM_ALERTS,
                    "Exam Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Countdown alerts and revision deadlines for upcoming exams"
                }
            )

            channels.forEach { nm.createNotificationChannel(it) }
        }
    }

    fun getSettings(context: Context): AppNotificationSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppNotificationSettings(
            studyReminders = prefs.getBoolean("study_reminders", true),
            motivation = prefs.getBoolean("motivation", true),
            focusShield = prefs.getBoolean("focus_shield", true),
            progress = prefs.getBoolean("progress", true),
            examAlerts = prefs.getBoolean("exam_alerts", true),
            quietHoursEnabled = prefs.getBoolean("quiet_hours_enabled", true),
            quietHoursStartHour = prefs.getInt("quiet_start_h", 22),
            quietHoursStartMinute = prefs.getInt("quiet_start_m", 0),
            quietHoursEndHour = prefs.getInt("quiet_end_h", 7),
            quietHoursEndMinute = prefs.getInt("quiet_end_m", 0),
            reminderHour = prefs.getInt("reminder_h", 18),
            reminderMinute = prefs.getInt("reminder_m", 30),
            dailyMotivationLimit = prefs.getInt("daily_limit", 1),
            reminderBehavior = prefs.getString("behavior", "Gentle") ?: "Gentle"
        )
    }

    fun saveSettings(context: Context, settings: AppNotificationSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("study_reminders", settings.studyReminders)
            .putBoolean("motivation", settings.motivation)
            .putBoolean("focus_shield", settings.focusShield)
            .putBoolean("progress", settings.progress)
            .putBoolean("exam_alerts", settings.examAlerts)
            .putBoolean("quiet_hours_enabled", settings.quietHoursEnabled)
            .putInt("quiet_start_h", settings.quietHoursStartHour)
            .putInt("quiet_start_m", settings.quietHoursStartMinute)
            .putInt("quiet_end_h", settings.quietHoursEndHour)
            .putInt("quiet_end_m", settings.quietHoursEndMinute)
            .putInt("reminder_h", settings.reminderHour)
            .putInt("reminder_m", settings.reminderMinute)
            .putInt("daily_limit", settings.dailyMotivationLimit)
            .putString("behavior", settings.reminderBehavior)
            .apply()

        // Reschedule alarm
        scheduleDailyStudyReminder(context, settings.reminderHour, settings.reminderMinute)
    }

    fun isInQuietHours(context: Context): Boolean {
        val settings = getSettings(context)
        if (!settings.quietHoursEnabled) return false

        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = settings.quietHoursStartHour * 60 + settings.quietHoursStartMinute
        val endMinutes = settings.quietHoursEndHour * 60 + settings.quietHoursEndMinute

        return if (startMinutes > endMinutes) {
            // Over midnight (e.g., 22:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes until endMinutes
        }
    }

    fun scheduleDailyStudyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = "com.example.studymate.DAILY_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            // Handled gracefully
        }
    }

    fun sendStudyTimeReminder(context: Context, subject: String = "Physics", topic: String = "Current Electricity") {
        if (isInQuietHours(context)) return
        val settings = getSettings(context)
        if (!settings.studyReminders) return

        // Content intent -> opens MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
            putExtra("EXTRA_SUBJECT", subject)
            putExtra("EXTRA_TOPIC", topic)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            2001,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Start Focus
        val startFocusIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_START_FOCUS_IMMEDIATELY", true)
            putExtra("EXTRA_SUBJECT", subject)
            putExtra("EXTRA_TOPIC", topic)
        }
        val startFocusPendingIntent = PendingIntent.getActivity(
            context,
            2002,
            startFocusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_STUDY
            putExtra("EXTRA_SUBJECT", subject)
            putExtra("EXTRA_TOPIC", topic)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2003,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STUDY_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("📚 Study time!")
            .setContentText("Your study session has started. Let's make this time count. 🚀")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your study session on $subject ($topic) has started. Let's make this time count. 🚀")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start Focus", startFocusPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(201, builder.build())
        } catch (e: SecurityException) {
            // Permission check
        }
    }

    fun sendMissedStudyReminder(context: Context, subject: String = "Physics") {
        if (isInQuietHours(context)) return
        val settings = getSettings(context)
        if (!settings.studyReminders) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STUDY_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("👀 Your study session is still waiting")
            .setContentText("Ready to start a quick 25-minute focus session?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(301, builder.build())
        } catch (e: SecurityException) {
            // Handled gracefully
        }
    }

    fun sendMotivationalNotification(context: Context, customQuote: String? = null) {
        if (isInQuietHours(context)) return
        val settings = getSettings(context)
        if (!settings.motivation) return

        val quote = customQuote ?: MOTIVATIONAL_QUOTES.random()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            4001,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MOTIVATION)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Daily Motivation ✨")
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(401, builder.build())
        } catch (e: SecurityException) {
            // Handled gracefully
        }
    }

    fun sendFocusCompletionNotification(context: Context, minutes: Int, xpEarned: Int) {
        val settings = getSettings(context)
        if (!settings.focusShield) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "PROGRESS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            5001,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS_SHIELD)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle("🎉 Focus Session Complete!")
            .setContentText("Great work! You focused for $minutes minutes. (+${xpEarned} XP)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Great work! You completed $minutes minutes of deep, distraction-free study. You earned +$xpEarned XP. 🚀")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(501, builder.build())
        } catch (e: SecurityException) {
            // Handled gracefully
        }
    }

    fun sendExamAlert(context: Context, examName: String, daysLeft: Int) {
        val settings = getSettings(context)
        if (!settings.examAlerts) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "STUDY")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            6001,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_EXAM_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏳ $daysLeft Days until $examName")
            .setContentText("Review high-priority topics today to stay ahead of schedule.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(601, builder.build())
        } catch (e: SecurityException) {
            // Handled gracefully
        }
    }
}
