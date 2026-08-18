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
import java.text.SimpleDateFormat
import java.util.*

data class AppNotificationSettings(
    val masterEnabled: Boolean = true,
    val studySessionReminders: Boolean = true,
    val examCountdownAlerts: Boolean = true,
    val dailyGoalReminders: Boolean = true,
    val missedStudyReminders: Boolean = true,
    val breakReminders: Boolean = true,
    val focusStartedAlerts: Boolean = true,
    val focusCompletedAlerts: Boolean = true,
    val dailyMotivationAlerts: Boolean = true,
    val studyReminderHour: Int = 19, // 7:00 PM
    val studyReminderMinute: Int = 0,
    val dailyGoalReminderHour: Int = 20, // 8:30 PM
    val dailyGoalReminderMinute: Int = 30,
    val motivationReminderHour: Int = 8, // 8:00 AM
    val motivationReminderMinute: Int = 0,
    val activeReminderDays: Set<String> = setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val motivationFrequency: String = "Once Daily (Morning)", // Once Daily, Twice Daily, 3x Daily
    val quietHoursEnabled: Boolean = true,
    val quietHoursStartHour: Int = 22, // 10:00 PM
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,   // 7:00 AM
    val quietHoursEndMinute: Int = 0,
    val antiSpamCooldownMinutes: Int = 30
)

object StudyNotificationManager {
    // Channel IDs
    const val CHANNEL_STUDY_REMINDERS = "study_reminders"
    const val CHANNEL_EXAM_COUNTDOWN = "exam_countdown"
    const val CHANNEL_DAILY_GOALS = "daily_goals"
    const val CHANNEL_MISSED_STUDY = "missed_study"
    const val CHANNEL_BREAK_REMINDERS = "break_reminders"
    const val CHANNEL_FOCUS_ACTIVE = "focus_active"
    const val CHANNEL_FOCUS_COMPLETED = "focus_completed"
    const val CHANNEL_DAILY_MOTIVATION = "daily_motivation"

    // Action Intents
    const val ACTION_DAILY_STUDY_REMINDER = "com.example.studymate.ACTION_DAILY_STUDY_REMINDER"
    const val ACTION_DAILY_GOAL_REMINDER = "com.example.studymate.ACTION_DAILY_GOAL_REMINDER"
    const val ACTION_EXAM_COUNTDOWN_REMINDER = "com.example.studymate.ACTION_EXAM_COUNTDOWN_REMINDER"
    const val ACTION_DAILY_MOTIVATION_REMINDER = "com.example.studymate.ACTION_DAILY_MOTIVATION_REMINDER"
    const val ACTION_MISSED_STUDY_CHECK = "com.example.studymate.ACTION_MISSED_STUDY_CHECK"
    const val ACTION_BREAK_REMINDER = "com.example.studymate.ACTION_BREAK_REMINDER"
    const val ACTION_SNOOZE_STUDY = "com.example.studymate.ACTION_SNOOZE_STUDY"
    const val ACTION_SNOOZED_REMINDER = "com.example.studymate.ACTION_SNOOZED_REMINDER"

    // Notification IDs
    private const val NOTIF_ID_STUDY_SESSION = 1001
    private const val NOTIF_ID_EXAM_COUNTDOWN = 1002
    private const val NOTIF_ID_DAILY_GOAL = 1003
    private const val NOTIF_ID_MISSED_STUDY = 1004
    private const val NOTIF_ID_BREAK_REMINDER = 1005
    private const val NOTIF_ID_FOCUS_ACTIVE = 1006
    private const val NOTIF_ID_FOCUS_COMPLETED = 1007
    private const val NOTIF_ID_MOTIVATION = 1008
    private const val NOTIF_ID_STUDY_REMINDER = 1009
    private const val NOTIF_ID_APP_USAGE = 1010

    private const val PREFS_NAME = "studymate_notifications_prefs"
    private const val SPAM_PREFS = "studymate_spam_cooldown_prefs"

    val MOTIVATIONAL_QUOTES = listOf(
        "💪 One focused session today can move you closer to your target rank.",
        "📚 Small, deliberate progress every day compounds into mastery.",
        "🚀 Your future self will be grateful for the discipline you show right now.",
        "🧠 Consistency beats raw intensity. Keep showing up every single day!",
        "🌟 You don't have to feel like studying to get started—start small for 5 minutes.",
        "🎯 Clear your desk, breathe deeply, and give this topic 25 minutes of full focus.",
        "✨ Active recall and spaced practice turn temporary facts into lifelong knowledge.",
        "🔥 The pain of discipline weighs ounces, but the pain of regret weighs tons.",
        "⚡ Every problem you solve today makes the final exam that much easier."
    )

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_STUDY_REMINDERS,
                    "Study Session Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when scheduled study sessions begin with quick Start & Snooze actions"
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_EXAM_COUNTDOWN,
                    "Exam Countdown Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Days remaining countdowns and high-yield topic prompts for your target exam"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_DAILY_GOALS,
                    "Daily Study Goal Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily target minute reminders, flashcard goals, and mission updates"
                },
                NotificationChannel(
                    CHANNEL_MISSED_STUDY,
                    "Missed-Study Catch-up Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Gentle, non-intrusive reminders if you missed your scheduled study slot"
                },
                NotificationChannel(
                    CHANNEL_BREAK_REMINDERS,
                    "Break & Rest Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Prompts to stretch, hydrate, and relax your eyes between study sprints"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_FOCUS_ACTIVE,
                    "Focus Session Active Alerts",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Live ongoing status during deep focus countdowns and App Shield"
                },
                NotificationChannel(
                    CHANNEL_FOCUS_COMPLETED,
                    "Focus Session Celebrations",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "XP rewards and session completion summaries after deep focus"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_DAILY_MOTIVATION,
                    "Daily Motivation Boosts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Personalized inspiring quotes, mindset tips, and positive encouragement"
                }
            )

            channels.forEach { nm.createNotificationChannel(it) }
        }
    }

    fun getSettings(context: Context): AppNotificationSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDays = prefs.getStringSet("active_days", null)
            ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        return AppNotificationSettings(
            masterEnabled = prefs.getBoolean("master_enabled", true),
            studySessionReminders = prefs.getBoolean("study_session_reminders", true),
            examCountdownAlerts = prefs.getBoolean("exam_countdown_alerts", true),
            dailyGoalReminders = prefs.getBoolean("daily_goal_reminders", true),
            missedStudyReminders = prefs.getBoolean("missed_study_reminders", true),
            breakReminders = prefs.getBoolean("break_reminders", true),
            focusStartedAlerts = prefs.getBoolean("focus_started_alerts", true),
            focusCompletedAlerts = prefs.getBoolean("focus_completed_alerts", true),
            dailyMotivationAlerts = prefs.getBoolean("daily_motivation_alerts", true),
            studyReminderHour = prefs.getInt("study_h", 19),
            studyReminderMinute = prefs.getInt("study_m", 0),
            dailyGoalReminderHour = prefs.getInt("goal_h", 20),
            dailyGoalReminderMinute = prefs.getInt("goal_m", 30),
            motivationReminderHour = prefs.getInt("mot_h", 8),
            motivationReminderMinute = prefs.getInt("mot_m", 0),
            activeReminderDays = savedDays,
            motivationFrequency = prefs.getString("mot_freq", "Once Daily (Morning)") ?: "Once Daily (Morning)",
            quietHoursEnabled = prefs.getBoolean("quiet_enabled", true),
            quietHoursStartHour = prefs.getInt("quiet_start_h", 22),
            quietHoursStartMinute = prefs.getInt("quiet_start_m", 0),
            quietHoursEndHour = prefs.getInt("quiet_end_h", 7),
            quietHoursEndMinute = prefs.getInt("quiet_end_m", 0),
            antiSpamCooldownMinutes = prefs.getInt("cooldown_mins", 30)
        )
    }

    fun saveSettings(context: Context, settings: AppNotificationSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("master_enabled", settings.masterEnabled)
            .putBoolean("study_session_reminders", settings.studySessionReminders)
            .putBoolean("exam_countdown_alerts", settings.examCountdownAlerts)
            .putBoolean("daily_goal_reminders", settings.dailyGoalReminders)
            .putBoolean("missed_study_reminders", settings.missedStudyReminders)
            .putBoolean("break_reminders", settings.breakReminders)
            .putBoolean("focus_started_alerts", settings.focusStartedAlerts)
            .putBoolean("focus_completed_alerts", settings.focusCompletedAlerts)
            .putBoolean("daily_motivation_alerts", settings.dailyMotivationAlerts)
            .putInt("study_h", settings.studyReminderHour)
            .putInt("study_m", settings.studyReminderMinute)
            .putInt("goal_h", settings.dailyGoalReminderHour)
            .putInt("goal_m", settings.dailyGoalReminderMinute)
            .putInt("mot_h", settings.motivationReminderHour)
            .putInt("mot_m", settings.motivationReminderMinute)
            .putStringSet("active_days", settings.activeReminderDays)
            .putString("mot_freq", settings.motivationFrequency)
            .putBoolean("quiet_enabled", settings.quietHoursEnabled)
            .putInt("quiet_start_h", settings.quietHoursStartHour)
            .putInt("quiet_start_m", settings.quietHoursStartMinute)
            .putInt("quiet_end_h", settings.quietHoursEndHour)
            .putInt("quiet_end_m", settings.quietHoursEndMinute)
            .putInt("cooldown_mins", settings.antiSpamCooldownMinutes)
            .apply()

        // Reschedule all alarms based on updated settings
        scheduleAllReminders(context)
    }

    fun isInQuietHours(context: Context): Boolean {
        val settings = getSettings(context)
        if (!settings.quietHoursEnabled) return false

        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = settings.quietHoursStartHour * 60 + settings.quietHoursStartMinute
        val endMinutes = settings.quietHoursEndHour * 60 + settings.quietHoursEndMinute

        return if (startMinutes > endMinutes) {
            // Spans overnight (e.g. 22:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes until endMinutes
        }
    }

    fun isTodayActiveDay(context: Context): Boolean {
        val settings = getSettings(context)
        val cal = Calendar.getInstance()
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> "Mon"
        }
        return settings.activeReminderDays.contains(dayOfWeek)
    }

    private fun isSpamCooldownActive(context: Context, key: String, cooldownMinutes: Int): Boolean {
        val prefs = context.getSharedPreferences(SPAM_PREFS, Context.MODE_PRIVATE)
        val lastSent = prefs.getLong("last_sent_$key", 0L)
        val now = System.currentTimeMillis()
        val cooldownMillis = cooldownMinutes * 60 * 1000L
        return (now - lastSent) < cooldownMillis
    }

    private fun markNotificationSent(context: Context, key: String) {
        val prefs = context.getSharedPreferences(SPAM_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong("last_sent_$key", System.currentTimeMillis()).apply()
    }

    // =========================================================================
    // 1. Study Session Reminder
    // =========================================================================
    fun sendStudySessionReminder(
        context: Context,
        userName: String = "Rahul",
        subject: String = "Physics",
        topic: String = "Current Electricity",
        timeString: String = "7:00 PM",
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.studySessionReminders) return
            if (isInQuietHours(context)) return
            if (!isTodayActiveDay(context)) return
            if (isSpamCooldownActive(context, "study_session", settings.antiSpamCooldownMinutes)) return
        }

        val name = userName.ifBlank { "Scholar" }
        val title = "Hey $name 👋, your $timeString study session starts now 📚🔥"
        val body = "Time to dive into $subject: $topic. Let's make today's progress count!"

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

        // Action: Snooze 15m
        val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_STUDY
            putExtra("EXTRA_USER_NAME", name)
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
            .setContentTitle("📚 Study Session Starts Now")
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📚 $title")
                    .bigText("$body\n\n🎯 Priority Subject: $subject\n📖 Topic: $topic")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start Focus", startFocusPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 15m", snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_STUDY_SESSION, builder.build())
            markNotificationSent(context, "study_session")
        } catch (e: SecurityException) {
            // Missing runtime permission
        }
    }

    // =========================================================================
    // 2. Exam Countdown Reminder
    // =========================================================================
    fun sendExamCountdownReminder(
        context: Context,
        userName: String = "Rahul",
        examName: String = "JEE Main / Board Exam",
        daysLeft: Int = 30,
        targetScore: String = "Top 500 AIR",
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.examCountdownAlerts) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "exam_countdown", settings.antiSpamCooldownMinutes)) return
        }

        val name = userName.ifBlank { "Scholar" }
        val title = "Only $daysLeft days left for your $examName ⏳"
        val body = "Hey $name 👋, one focused session today can make a massive difference. Target: $targetScore 💙"

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "STUDY")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2101,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_EXAM_COUNTDOWN)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏳ Exam Countdown: $daysLeft Days")
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("⏳ $title")
                    .bigText("$body\n\n🎯 Consistent daily active recall will get you across the finish line with confidence.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_today, "Open Study Plan", pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_EXAM_COUNTDOWN, builder.build())
            markNotificationSent(context, "exam_countdown")
        } catch (e: SecurityException) {
            // Handled
        }
    }

    // =========================================================================
    // 3. Daily Study Goal Reminder
    // =========================================================================
    fun sendDailyGoalReminder(
        context: Context,
        userName: String = "Rahul",
        dailyGoal: String = "Complete daily scheduled topics & 20 flashcards",
        targetMinutes: Int = 180,
        completedMinutes: Int = 90,
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.dailyGoalReminders) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "daily_goal", settings.antiSpamCooldownMinutes)) return
        }

        val name = userName.ifBlank { "Scholar" }
        val remainingMins = (targetMinutes - completedMinutes).coerceAtLeast(0)
        val title = "Hey $name 🎯, check in on your daily study goal"
        val body = "Target Goal: \"$dailyGoal\"\n⏱️ $remainingMins mins left to reach today's target ($completedMinutes / $targetMinutes mins completed)."

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "HOME")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2201,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_GOALS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("🎯 Daily Study Goal Check-in")
            .setContentText("Hey $name, you have $remainingMins mins left for today's goal!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🎯 Daily Study Goal Progress")
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_DAILY_GOAL, builder.build())
            markNotificationSent(context, "daily_goal")
        } catch (e: SecurityException) {
            // Handled
        }
    }

    // =========================================================================
    // 4. Missed-Study Reminder
    // =========================================================================
    fun sendMissedStudyReminder(
        context: Context,
        userName: String = "Rahul",
        subject: String = "Physics",
        topic: String = "Current Electricity",
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.missedStudyReminders) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "missed_study", settings.antiSpamCooldownMinutes)) return
        }

        val name = userName.ifBlank { "Scholar" }
        val title = "Hey $name 👀, your $subject study session is waiting"
        val body = "Ready for a quick 25-minute focus session on $topic? Small steps keep your streak alive! ⚡"

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
            putExtra("EXTRA_SUBJECT", subject)
            putExtra("EXTRA_TOPIC", topic)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2301,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED_STUDY)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("👀 Study Session Still Waiting")
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("👀 Catch-up Reminder")
                    .bigText("$body\n\n💪 Even 15-20 minutes of review now protects your knowledge retention.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start 25m Sprint", pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_MISSED_STUDY, builder.build())
            markNotificationSent(context, "missed_study")
        } catch (e: SecurityException) {
            // Handled
        }
    }

    // =========================================================================
    // 5. Break Reminder
    // =========================================================================
    fun sendBreakReminder(
        context: Context,
        userName: String = "Rahul",
        breakMinutes: Int = 15,
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.breakReminders) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "break_reminder", 10)) return
        }

        val name = userName.ifBlank { "Scholar" }
        val title = "Time for a break, $name! 🧘"
        val body = "Take $breakMinutes minutes to stretch, hydrate 💧, and rest your eyes before the next study sprint."

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2401,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_BREAK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🧘 Mindful Break Reminder")
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🧘 $title")
                    .bigText("$body\n\n💡 Pro tip: Look at an object 20 feet away for 20 seconds to reset visual fatigue.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_BREAK_REMINDER, builder.build())
            markNotificationSent(context, "break_reminder")
        } catch (e: SecurityException) {
            // Handled
        }
    }

    // =========================================================================
    // 6. Focus Session Started Notification
    // =========================================================================
    fun sendFocusSessionStarted(
        context: Context,
        userName: String = "Rahul",
        subject: String = "Physics",
        topic: String = "Current Electricity",
        durationMinutes: Int = 25,
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.focusStartedAlerts) return
        }

        val name = userName.ifBlank { "Scholar" }
        val title = "🎯 Focus Session Active: $subject"
        val body = "Hey $name, deep focus timer is running for $topic ($durationMinutes min). Focus Shield is safeguarding your attention."

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2501,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS_ACTIVE)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("$durationMinutes min focus on $topic")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_FOCUS_ACTIVE, builder.build())
        } catch (e: SecurityException) {
            // Handled
        }
    }

    // =========================================================================
    // 7. Focus Session Completed Notification
    // =========================================================================
    fun sendFocusSessionCompleted(
        context: Context,
        userName: String = "Rahul",
        subject: String = "Physics",
        minutes: Int = 25,
        xpEarned: Int = 50,
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.focusCompletedAlerts) return
        }

        val name = userName.ifBlank { "Scholar" }
        val title = "🎉 Focus Session Completed! (+${xpEarned} XP)"
        val body = "Great work $name! You completed $minutes minutes of deep, distraction-free study on $subject. Streak updated! 🚀"

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "PROGRESS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2601,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS_COMPLETED)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle(title)
            .setContentText("You completed $minutes minutes of $subject study. (+${xpEarned} XP)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("$body\n\n🏆 Keep this momentum going for your upcoming milestones!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "View Progress", pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_FOCUS_COMPLETED, builder.build())
        } catch (e: SecurityException) {
            // Handled
        }
    }

    // =========================================================================
    // 8. Daily Motivational Notification
    // =========================================================================
    fun sendDailyMotivationalNotification(
        context: Context,
        userName: String = "Rahul",
        examName: String = "JEE / Board Exam",
        customQuote: String? = null,
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.dailyMotivationAlerts) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "motivation", settings.antiSpamCooldownMinutes)) return
        }

        val name = userName.ifBlank { "Scholar" }
        val quote = customQuote ?: MOTIVATIONAL_QUOTES.random()
        val title = "Daily Motivation for $name ✨"
        val body = "$quote\n\n🎯 Stay dedicated to your $examName goal today!"

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2701,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_MOTIVATION)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("✨ Daily Motivation for $name")
            .setContentText(quote)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("✨ Daily Motivation for $name")
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_MOTIVATION, builder.build())
            markNotificationSent(context, "motivation")
        } catch (e: SecurityException) {
            // Handled
        }
    }

    // =========================================================================
    // Master Scheduler: Uses AlarmManager for Scheduled Reminders
    // =========================================================================
    fun scheduleAllReminders(context: Context) {
        val settings = getSettings(context)
        if (!settings.masterEnabled) {
            cancelAllReminders(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Study Session Reminder Alarm
        if (settings.studySessionReminders) {
            scheduleDailyAlarm(
                context,
                alarmManager,
                requestCode = 3001,
                action = ACTION_DAILY_STUDY_REMINDER,
                hour = settings.studyReminderHour,
                minute = settings.studyReminderMinute
            )
        } else {
            cancelAlarm(context, alarmManager, 3001, ACTION_DAILY_STUDY_REMINDER)
        }

        // 2. Daily Goal Reminder Alarm
        if (settings.dailyGoalReminders) {
            scheduleDailyAlarm(
                context,
                alarmManager,
                requestCode = 3002,
                action = ACTION_DAILY_GOAL_REMINDER,
                hour = settings.dailyGoalReminderHour,
                minute = settings.dailyGoalReminderMinute
            )
        } else {
            cancelAlarm(context, alarmManager, 3002, ACTION_DAILY_GOAL_REMINDER)
        }

        // 3. Daily Motivational Alarm(s) based on Frequency
        if (settings.dailyMotivationAlerts) {
            scheduleDailyAlarm(
                context,
                alarmManager,
                requestCode = 3003,
                action = ACTION_DAILY_MOTIVATION_REMINDER,
                hour = settings.motivationReminderHour,
                minute = settings.motivationReminderMinute
            )

            // If twice daily, also schedule evening motivation (6 PM)
            if (settings.motivationFrequency.contains("Twice", ignoreCase = true) ||
                settings.motivationFrequency.contains("3x", ignoreCase = true)
            ) {
                scheduleDailyAlarm(
                    context,
                    alarmManager,
                    requestCode = 3004,
                    action = ACTION_DAILY_MOTIVATION_REMINDER,
                    hour = 18,
                    minute = 0
                )
            } else {
                cancelAlarm(context, alarmManager, 3004, ACTION_DAILY_MOTIVATION_REMINDER)
            }

            // If 3x daily, also schedule afternoon motivation (2 PM)
            if (settings.motivationFrequency.contains("3x", ignoreCase = true)) {
                scheduleDailyAlarm(
                    context,
                    alarmManager,
                    requestCode = 3005,
                    action = ACTION_DAILY_MOTIVATION_REMINDER,
                    hour = 14,
                    minute = 0
                )
            } else {
                cancelAlarm(context, alarmManager, 3005, ACTION_DAILY_MOTIVATION_REMINDER)
            }
        } else {
            cancelAlarm(context, alarmManager, 3003, ACTION_DAILY_MOTIVATION_REMINDER)
            cancelAlarm(context, alarmManager, 3004, ACTION_DAILY_MOTIVATION_REMINDER)
            cancelAlarm(context, alarmManager, 3005, ACTION_DAILY_MOTIVATION_REMINDER)
        }

        // 4. Daily Exam Countdown & Missed Study Check Alarm (Runs at 9:00 AM)
        if (settings.examCountdownAlerts) {
            scheduleDailyAlarm(
                context,
                alarmManager,
                requestCode = 3006,
                action = ACTION_EXAM_COUNTDOWN_REMINDER,
                hour = 9,
                minute = 0
            )
        } else {
            cancelAlarm(context, alarmManager, 3006, ACTION_EXAM_COUNTDOWN_REMINDER)
        }

        // 5. Missed-study check (Runs 45 mins after scheduled study reminder)
        if (settings.missedStudyReminders) {
            val missedHour = (settings.studyReminderHour + (if (settings.studyReminderMinute + 45 >= 60) 1 else 0)) % 24
            val missedMinute = (settings.studyReminderMinute + 45) % 60
            scheduleDailyAlarm(
                context,
                alarmManager,
                requestCode = 3007,
                action = ACTION_MISSED_STUDY_CHECK,
                hour = missedHour,
                minute = missedMinute
            )
        } else {
            cancelAlarm(context, alarmManager, 3007, ACTION_MISSED_STUDY_CHECK)
        }
    }

    private fun scheduleDailyAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        action: String,
        hour: Int,
        minute: Int
    ) {
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Inexact fallback
            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (ignored: Exception) {}
        }
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        action: String
    ) {
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val codes = listOf(3001, 3002, 3003, 3004, 3005, 3006, 3007)
        val actions = listOf(
            ACTION_DAILY_STUDY_REMINDER,
            ACTION_DAILY_GOAL_REMINDER,
            ACTION_DAILY_MOTIVATION_REMINDER,
            ACTION_EXAM_COUNTDOWN_REMINDER,
            ACTION_MISSED_STUDY_CHECK
        )
        codes.forEach { code ->
            actions.forEach { act ->
                cancelAlarm(context, alarmManager, code, act)
            }
        }
    }

    // =========================================================================
    // NOVA PERSONAL AI ASSISTANT PROACTIVE NOTIFICATIONS
    // =========================================================================

    fun sendNovaStudyReminder(
        context: Context,
        userName: String = "Boss",
        subject: String = "Physics",
        topic: String = "Current Electricity",
        durationMins: Int = 25,
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.studySessionReminders) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "nova_study_reminder", settings.antiSpamCooldownMinutes)) return
        }

        val title = "Boss 😄 $subject ka time ho gaya 📚"
        val body = "Aaj $topic ka session pending hai. $durationMins minutes se start karein?"

        val startFocusIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
            putExtra("AUTO_START_FOCUS", true)
            putExtra("FOCUS_SUBJECT", subject)
            putExtra("FOCUS_TOPIC", topic)
            putExtra("FOCUS_DURATION", durationMins)
        }
        val startFocusPendingIntent = PendingIntent.getActivity(
            context,
            2401,
            startFocusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_STUDY
            putExtra("EXTRA_SUBJECT", subject)
            putExtra("EXTRA_TOPIC", topic)
            putExtra("EXTRA_DURATION", durationMins)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2402,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STUDY_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(startFocusPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start Study ($durationMins m)", startFocusPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 10 min", snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_STUDY_REMINDER, builder.build())
            markNotificationSent(context, "nova_study_reminder")
        } catch (e: SecurityException) {
            // Handled
        }
    }

    fun sendNovaMissedStudyAlert(
        context: Context,
        userName: String = "Boss",
        subject: String = "Physics",
        topic: String = "Current Electricity",
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled || !settings.missedStudyReminders) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "nova_missed_study", settings.antiSpamCooldownMinutes)) return
        }

        val title = "Boss, aaj ka $subject session miss ho gaya 😅"
        val body = "Agar free ho to abhi 20 minutes ka quick recovery session kar sakte hain."

        val startNowIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
            putExtra("AUTO_START_FOCUS", true)
            putExtra("FOCUS_SUBJECT", subject)
            putExtra("FOCUS_TOPIC", topic)
            putExtra("FOCUS_DURATION", 20)
        }
        val startNowPendingIntent = PendingIntent.getActivity(
            context,
            2411,
            startNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val planIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "PLANNER")
        }
        val planPendingIntent = PendingIntent.getActivity(
            context,
            2412,
            planIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED_STUDY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(startNowPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start Now (20m)", startNowPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "Reschedule", planPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_MISSED_STUDY, builder.build())
            markNotificationSent(context, "nova_missed_study")
        } catch (e: SecurityException) {
            // Handled
        }
    }

    fun sendNovaExcessiveAppUsageAlert(
        context: Context,
        userName: String = "Boss",
        appName: String = "YouTube",
        pendingSubject: String = "Physics",
        durationMins: Int = 25,
        isTest: Boolean = false
    ) {
        val settings = getSettings(context)
        if (!isTest) {
            if (!settings.masterEnabled) return
            if (isInQuietHours(context)) return
            if (isSpamCooldownActive(context, "nova_excessive_usage", 45)) return
        }

        val title = "Boss 😅 $appName par kaafi time ho gaya"
        val body = "Aaj ka $pendingSubject session abhi pending hai. Chalo $durationMins minutes padh lete hain? 📚"

        val startFocusIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
            putExtra("AUTO_START_FOCUS", true)
            putExtra("FOCUS_SUBJECT", pendingSubject)
            putExtra("FOCUS_DURATION", durationMins)
        }
        val startFocusPendingIntent = PendingIntent.getActivity(
            context,
            2421,
            startFocusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STUDY_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(startFocusPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start Study ($durationMins m)", startFocusPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_STUDY_REMINDER + 10, builder.build())
            markNotificationSent(context, "nova_excessive_usage")
        } catch (e: SecurityException) {
            // Handled
        }
    }
}
