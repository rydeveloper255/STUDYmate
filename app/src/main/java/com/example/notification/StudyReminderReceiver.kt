package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.StudyMateApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StudyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? StudyMateApplication
                val studyRepo = app?.studyRepository
                val userProfile = studyRepo?.userProfile?.firstOrNull()
                val planItems = studyRepo?.allPlanItems?.firstOrNull() ?: emptyList()

                val userName = userProfile?.name?.ifBlank { "Rahul" } ?: "Rahul"
                val examName = userProfile?.examName?.ifBlank { "Board & Competitive Exam" } ?: "Board & Competitive Exam"
                val targetScore = userProfile?.targetScore?.ifBlank { "Top 500 AIR" } ?: "Top 500 AIR"
                val dailyGoal = userProfile?.dailyStudyGoal?.ifBlank { "Complete daily scheduled topics & 20 flashcards" }
                    ?: "Complete daily scheduled topics & 20 flashcards"
                val targetMinutes = userProfile?.dailyTargetMinutes ?: 180
                val totalFocusMinutes = userProfile?.totalFocusMinutes ?: 0
                val breakMinutes = userProfile?.breakDurationMinutes ?: 15

                // Find next pending study topic or fallback to first subject
                val nextPending = planItems.firstOrNull { !it.isCompleted }
                val subject = nextPending?.subject ?: (userProfile?.subjects?.firstOrNull() ?: "Physics")
                val topic = nextPending?.topic ?: "Core Concept Mastery"

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.intent.action.QUICKBOOT_POWERON" -> {
                        StudyNotificationManager.scheduleAllReminders(context)
                    }

                    StudyNotificationManager.ACTION_DAILY_STUDY_REMINDER -> {
                        val cal = Calendar.getInstance()
                        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
                        StudyNotificationManager.sendStudySessionReminder(
                            context = context,
                            userName = userName,
                            subject = subject,
                            topic = topic,
                            timeString = timeStr
                        )
                    }

                    StudyNotificationManager.ACTION_DAILY_GOAL_REMINDER -> {
                        StudyNotificationManager.sendDailyGoalReminder(
                            context = context,
                            userName = userName,
                            dailyGoal = dailyGoal,
                            targetMinutes = targetMinutes,
                            completedMinutes = totalFocusMinutes
                        )
                    }

                    StudyNotificationManager.ACTION_EXAM_COUNTDOWN_REMINDER -> {
                        val examMillis = userProfile?.examDateMillis ?: (System.currentTimeMillis() + 30L * 86400000L)
                        val daysLeft = ((examMillis - System.currentTimeMillis()) / 86400000L).coerceAtLeast(1L).toInt()
                        StudyNotificationManager.sendExamCountdownReminder(
                            context = context,
                            userName = userName,
                            examName = examName,
                            daysLeft = daysLeft,
                            targetScore = targetScore
                        )
                    }

                    StudyNotificationManager.ACTION_DAILY_MOTIVATION_REMINDER -> {
                        StudyNotificationManager.sendDailyMotivationalNotification(
                            context = context,
                            userName = userName,
                            examName = examName
                        )
                    }

                    StudyNotificationManager.ACTION_MISSED_STUDY_CHECK -> {
                        // Check if user has zero focus minutes logged today or incomplete plan items
                        if (totalFocusMinutes < 15) {
                            StudyNotificationManager.sendMissedStudyReminder(
                                context = context,
                                userName = userName,
                                subject = subject,
                                topic = topic
                            )
                        }
                    }

                    StudyNotificationManager.ACTION_BREAK_REMINDER -> {
                        StudyNotificationManager.sendBreakReminder(
                            context = context,
                            userName = userName,
                            breakMinutes = breakMinutes
                        )
                    }

                    StudyNotificationManager.ACTION_SNOOZE_STUDY -> {
                        val sSubject = intent.getStringExtra("EXTRA_SUBJECT") ?: subject
                        val sTopic = intent.getStringExtra("EXTRA_TOPIC") ?: topic
                        val sName = intent.getStringExtra("EXTRA_USER_NAME") ?: userName

                        // Snooze for 15 minutes
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                        if (alarmManager != null) {
                            val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
                                this.action = StudyNotificationManager.ACTION_SNOOZED_REMINDER
                                putExtra("EXTRA_USER_NAME", sName)
                                putExtra("EXTRA_SUBJECT", sSubject)
                                putExtra("EXTRA_TOPIC", sTopic)
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
                    }

                    StudyNotificationManager.ACTION_SNOOZED_REMINDER -> {
                        val sSubject = intent.getStringExtra("EXTRA_SUBJECT") ?: subject
                        val sTopic = intent.getStringExtra("EXTRA_TOPIC") ?: topic
                        val sName = intent.getStringExtra("EXTRA_USER_NAME") ?: userName
                        val cal = Calendar.getInstance()
                        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(cal.time)

                        StudyNotificationManager.sendStudySessionReminder(
                            context = context,
                            userName = sName,
                            subject = sSubject,
                            topic = sTopic,
                            timeString = timeStr
                        )
                    }

                    NovaNotificationEngine.ACTION_START_STUDY -> {
                        val sSubject = intent.getStringExtra(NovaNotificationEngine.EXTRA_SUBJECT) ?: subject
                        val sTopic = intent.getStringExtra(NovaNotificationEngine.EXTRA_TOPIC) ?: topic
                        val sMins = intent.getIntExtra(NovaNotificationEngine.EXTRA_MINUTES, 25)

                        val mainIntent = Intent(context, com.example.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(NovaNotificationEngine.EXTRA_SUBJECT, sSubject)
                            putExtra(NovaNotificationEngine.EXTRA_TOPIC, sTopic)
                            putExtra(NovaNotificationEngine.EXTRA_MINUTES, sMins)
                        }
                        context.startActivity(mainIntent)
                    }

                    NovaNotificationEngine.ACTION_SKIP_STUDY -> {
                        // User chose to skip; no spamming, gracefully close notification
                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        nm?.cancel(2001)
                    }

                    StudyNotificationManager.ACTION_SCHEDULED_STUDY_PRE_15_MIN -> {
                        val sSubject = intent.getStringExtra("EXTRA_SUBJECT") ?: subject
                        val sTopic = intent.getStringExtra("EXTRA_TOPIC") ?: topic
                        val sTaskId = intent.getStringExtra("EXTRA_TASK_ID") ?: ""
                        StudyNotificationManager.sendScheduledStudyPre15MinNotification(
                            context = context,
                            subject = sSubject,
                            topic = sTopic,
                            taskId = sTaskId
                        )
                    }

                    StudyNotificationManager.ACTION_SCHEDULED_STUDY_EXACT_START -> {
                        val sSubject = intent.getStringExtra("EXTRA_SUBJECT") ?: subject
                        val sTopic = intent.getStringExtra("EXTRA_TOPIC") ?: topic
                        val sDuration = intent.getIntExtra("EXTRA_DURATION", 45)
                        val sStrict = intent.getBooleanExtra("EXTRA_STRICT", false)
                        val sTaskId = intent.getStringExtra("EXTRA_TASK_ID") ?: ""
                        StudyNotificationManager.sendScheduledStudyExactNotification(
                            context = context,
                            subject = sSubject,
                            topic = sTopic,
                            durationMinutes = sDuration,
                            isStrict = sStrict,
                            taskId = sTaskId
                        )
                    }
                }
            } catch (e: Exception) {
                // Handled gracefully
            } finally {
                pendingResult.finish()
            }
        }
    }
}
