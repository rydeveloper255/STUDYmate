package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StudyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        when (intent?.action) {
            "com.example.studymate.DAILY_REMINDER" -> {
                StudyNotificationManager.sendStudyTimeReminder(context)
                // Also send a motivational message if enabled
                StudyNotificationManager.sendMotivationalNotification(context)
            }
            StudyNotificationManager.ACTION_SNOOZE_STUDY -> {
                val subject = intent.getStringExtra("EXTRA_SUBJECT") ?: "Physics"
                val topic = intent.getStringExtra("EXTRA_TOPIC") ?: "Current Electricity"

                // Snooze for 15 minutes
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
                    action = "com.example.studymate.SNOOZED_REMINDER"
                    putExtra("EXTRA_SUBJECT", subject)
                    putExtra("EXTRA_TOPIC", topic)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    9001,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerTime = System.currentTimeMillis() + 15 * 60 * 1000L
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            "com.example.studymate.SNOOZED_REMINDER" -> {
                val subject = intent.getStringExtra("EXTRA_SUBJECT") ?: "Physics"
                val topic = intent.getStringExtra("EXTRA_TOPIC") ?: "Current Electricity"
                StudyNotificationManager.sendStudyTimeReminder(context, subject, topic)
            }
        }
    }
}
