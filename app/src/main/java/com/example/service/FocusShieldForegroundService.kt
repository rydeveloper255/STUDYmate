package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.notification.StudyNotificationManager
import kotlinx.coroutines.*
import java.util.Locale

/**
 * Robust Foreground Service managing the live Focus Timer & Shield lifecycle.
 * Ensures the session survives backgrounding, battery optimizations, and process recreation.
 */
class FocusShieldForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private lateinit var persistence: FocusSessionPersistence

    companion object {
        const val ACTION_START_FOCUS = "com.example.service.ACTION_START_FOCUS"
        const val ACTION_PAUSE_FOCUS = "com.example.service.ACTION_PAUSE_FOCUS"
        const val ACTION_RESUME_FOCUS = "com.example.service.ACTION_RESUME_FOCUS"
        const val ACTION_STOP_FOCUS = "com.example.service.ACTION_STOP_FOCUS"
        const val ACTION_SYNC_TIMER = "com.example.service.ACTION_SYNC_TIMER"

        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_TOPIC = "extra_topic"
        const val EXTRA_EXAM_NAME = "extra_exam_name"
        const val EXTRA_DURATION_MINS = "extra_duration_mins"
        const val EXTRA_STRICT_MODE = "extra_strict_mode"
        const val EXTRA_AUTO_STARTED = "extra_auto_started"
        const val EXTRA_SESSION_GOAL = "extra_session_goal"
        const val EXTRA_PLAN_ITEM_ID = "extra_plan_item_id"
        const val EXTRA_OCCURRENCE_ID = "extra_occurrence_id"

        private const val NOTIFICATION_ID = 1006

        fun startService(
            context: Context,
            subject: String,
            topic: String,
            durationMinutes: Int,
            examName: String = "",
            sessionGoal: String = "",
            planItemId: Long? = null,
            isStrictMode: Boolean = false,
            isAutoStarted: Boolean = false,
            occurrenceId: String? = null
        ) {
            val intent = Intent(context, FocusShieldForegroundService::class.java).apply {
                action = ACTION_START_FOCUS
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_TOPIC, topic)
                putExtra(EXTRA_DURATION_MINS, durationMinutes)
                putExtra(EXTRA_EXAM_NAME, examName)
                putExtra(EXTRA_SESSION_GOAL, sessionGoal)
                if (planItemId != null) putExtra(EXTRA_PLAN_ITEM_ID, planItemId)
                putExtra(EXTRA_STRICT_MODE, isStrictMode)
                putExtra(EXTRA_AUTO_STARTED, isAutoStarted)
                if (occurrenceId != null) putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseFocus(context: Context) {
            val intent = Intent(context, FocusShieldForegroundService::class.java).apply {
                action = ACTION_PAUSE_FOCUS
            }
            context.startService(intent)
        }

        fun resumeFocus(context: Context) {
            val intent = Intent(context, FocusShieldForegroundService::class.java).apply {
                action = ACTION_RESUME_FOCUS
            }
            context.startService(intent)
        }

        fun stopFocus(context: Context) {
            val intent = Intent(context, FocusShieldForegroundService::class.java).apply {
                action = ACTION_STOP_FOCUS
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        persistence = FocusSessionPersistence.getInstance(this)
        StudyNotificationManager.initNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOCUS -> {
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "General Study"
                val topic = intent.getStringExtra(EXTRA_TOPIC) ?: "Active Revision"
                val durationMins = intent.getIntExtra(EXTRA_DURATION_MINS, 25)
                val examName = intent.getStringExtra(EXTRA_EXAM_NAME) ?: "Competitive Exam"
                val sessionGoal = intent.getStringExtra(EXTRA_SESSION_GOAL) ?: "Complete $topic concepts"
                val rawPlanId = intent.getLongExtra(EXTRA_PLAN_ITEM_ID, -1L)
                val planItemId = if (rawPlanId != -1L) rawPlanId else null
                val isStrictMode = intent.getBooleanExtra(EXTRA_STRICT_MODE, false)
                val isAutoStarted = intent.getBooleanExtra(EXTRA_AUTO_STARTED, false)
                val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID)

                handleStartSession(
                    subject = subject,
                    topic = topic,
                    durationMinutes = durationMins,
                    examName = examName,
                    sessionGoal = sessionGoal,
                    planItemId = planItemId,
                    isStrictMode = isStrictMode,
                    isAutoStarted = isAutoStarted,
                    occurrenceId = occurrenceId
                )
            }
            ACTION_PAUSE_FOCUS -> {
                handlePauseSession()
            }
            ACTION_RESUME_FOCUS -> {
                handleResumeSession()
            }
            ACTION_STOP_FOCUS -> {
                handleStopSession()
            }
            ACTION_SYNC_TIMER -> {
                // Keep notification fresh
                val session = persistence.loadActiveSession()
                if (session != null && session.state == FocusSessionExecutionState.FOCUS_ACTIVE) {
                    updateForegroundNotification(session, session.calculateRemainingSeconds())
                }
            }
        }

        return START_STICKY
    }

    private fun handleStartSession(
        subject: String,
        topic: String,
        durationMinutes: Int,
        examName: String,
        sessionGoal: String,
        planItemId: Long?,
        isStrictMode: Boolean,
        isAutoStarted: Boolean,
        occurrenceId: String?
    ) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val restrictedPkgs = FocusShieldManager.getRestrictedPackages()

        val session = PersistedFocusSession(
            sessionId = java.util.UUID.randomUUID().toString(),
            subject = subject,
            topic = topic,
            examName = examName,
            sessionGoal = sessionGoal,
            planItemId = planItemId,
            plannedDurationMinutes = durationMinutes,
            startedAtTimestamp = nowWall,
            elapsedRealtimeStart = nowElapsed,
            pausedAccumulatedSeconds = 0L,
            pauseStartTimestamp = 0L,
            isPaused = false,
            isStrictMode = isStrictMode,
            isAutoStarted = isAutoStarted,
            occurrenceId = occurrenceId,
            state = FocusSessionExecutionState.FOCUS_ACTIVE,
            restrictedPackagesSnapshot = restrictedPkgs
        )

        persistence.saveActiveSession(session)

        // Mark auto-focus occurrence as executed so it won't duplicate
        if (occurrenceId != null) {
            persistence.recordOccurrenceExecuted(occurrenceId)
        }

        // Initialize and start FocusShieldManager blocking engine
        FocusShieldManager.startFocusSession(this, subject, topic, durationMinutes, isStrictMode)

        // Start Foreground Service with active notification
        val notif = buildNotification(session, durationMinutes * 60)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }

        startTimerTicker()
    }

    private fun handlePauseSession() {
        persistence.updatePauseState(true)
        val session = persistence.loadActiveSession()
        if (session != null) {
            updateForegroundNotification(session, session.calculateRemainingSeconds())
        }
    }

    private fun handleResumeSession() {
        persistence.updatePauseState(false)
        val session = persistence.loadActiveSession()
        if (session != null) {
            updateForegroundNotification(session, session.calculateRemainingSeconds())
        }
    }

    private fun handleStopSession() {
        timerJob?.cancel()
        timerJob = null
        FocusShieldManager.endFocusSession()
        persistence.clearActiveSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val session = persistence.loadActiveSession()
                if (session == null || session.state == FocusSessionExecutionState.IDLE) {
                    stopSelf()
                    break
                }

                if (session.state == FocusSessionExecutionState.FOCUS_ACTIVE && !session.isPaused) {
                    val remainingSecs = session.calculateRemainingSeconds()
                    FocusShieldManager.updateRemainingTime(remainingSecs)

                    if (remainingSecs <= 0) {
                        onSessionComplete(session)
                        break
                    } else {
                        // Refresh notification periodically
                        updateForegroundNotification(session, remainingSecs)
                    }
                }
            }
        }
    }

    private fun onSessionComplete(session: PersistedFocusSession) {
        val completedSession = session.copy(
            state = FocusSessionExecutionState.FOCUS_COMPLETED,
            isStrictMode = false // Strict mode MUST reset after completion
        )
        persistence.saveActiveSession(completedSession)
        FocusShieldManager.endFocusSession()

        // Send completion notification with reward
        StudyNotificationManager.sendFocusSessionCompleted(
            context = this,
            subject = session.subject,
            minutes = session.plannedDurationMinutes,
            xpEarned = session.plannedDurationMinutes * 2
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateForegroundNotification(session: PersistedFocusSession, remainingSecs: Int) {
        val notif = buildNotification(session, remainingSecs)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, notif)
    }

    private fun buildNotification(session: PersistedFocusSession, remainingSecs: Int): Notification {
        val minutes = remainingSecs / 60
        val seconds = remainingSecs % 60
        val timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)
        val totalSecs = (session.plannedDurationMinutes * 60).coerceAtLeast(1)
        val progress = ((totalSecs - remainingSecs).toFloat() / totalSecs * 100).toInt().coerceIn(0, 100)

        val statusPrefix = if (session.isPaused) "⏸️ PAUSED" else if (session.isStrictMode) "🛡️ STRICT FOCUS" else "🎯 FOCUS ACTIVE"
        val title = "$statusPrefix: ${session.subject} ($timeStr)"
        val content = if (session.isPaused) "Session paused • Tap to resume" else "${session.topic} • Focus Shield guarding attention"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FOCUS")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            3001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, StudyNotificationManager.CHANNEL_FOCUS_ACTIVE)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(timeStr)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Action: Pause / Resume
        if (session.isPaused) {
            val resumeIntent = Intent(this, FocusShieldForegroundService::class.java).apply {
                action = ACTION_RESUME_FOCUS
            }
            val resumePending = PendingIntent.getService(
                this,
                3002,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        } else {
            val pauseIntent = Intent(this, FocusShieldForegroundService::class.java).apply {
                action = ACTION_PAUSE_FOCUS
            }
            val pausePending = PendingIntent.getService(
                this,
                3003,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        }

        // Action: Stop / End Session (Only if not in Strict Mode)
        if (!session.isStrictMode) {
            val stopIntent = Intent(this, FocusShieldForegroundService::class.java).apply {
                action = ACTION_STOP_FOCUS
            }
            val stopPending = PendingIntent.getService(
                this,
                3004,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Session", stopPending)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
