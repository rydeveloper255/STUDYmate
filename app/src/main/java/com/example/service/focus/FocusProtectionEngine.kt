package com.example.service.focus

import android.content.Context
import android.util.Log
import com.example.service.FocusSessionExecutionState
import com.example.service.FocusSessionPersistence
import com.example.service.FocusShieldForegroundService
import com.example.service.PersistedFocusSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Focus Protection Engine 2.0 - Central Orchestrator & State Machine
 * Single Source of Truth for Focus Mode blocking, protection health, and session lifecycle.
 */
object FocusProtectionEngine {

    private const val TAG = "FocusProtectionEngine"

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Authoritative State Flows
    private val _sessionState = MutableStateFlow(FocusSessionState.IDLE)
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

    private val _activeSession = MutableStateFlow<PersistedFocusSession?>(null)
    val activeSession: StateFlow<PersistedFocusSession?> = _activeSession.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _healthState = MutableStateFlow<ProtectionHealthState?>(null)
    val healthState: StateFlow<ProtectionHealthState?> = _healthState.asStateFlow()

    private val _lastError = MutableStateFlow<FocusProtectionError?>(null)
    val lastError: StateFlow<FocusProtectionError?> = _lastError.asStateFlow()

    private val _telemetry = MutableStateFlow(FocusEngineTelemetry())
    val telemetry: StateFlow<FocusEngineTelemetry> = _telemetry.asStateFlow()

    private var usageMonitor: UsageMonitor? = null
    private var tickerJob: Job? = null
    private var appContext: Context? = null

    /**
     * Initializes the engine at application start.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        refreshHealthStatus(context)
        recoverSessionIfPossible(context)
    }

    /**
     * Genuine health check evaluation
     */
    fun refreshHealthStatus(context: Context): ProtectionHealthState {
        val health = PermissionHealthMonitor.getComprehensiveHealth(context)
        _healthState.value = health
        return health
    }

    /**
     * Checks if a session survived process death or app recreation.
     */
    fun recoverSessionIfPossible(context: Context) {
        val recovery = SessionRecovery(context)
        when (val result = recovery.evaluateRecovery()) {
            is SessionRecovery.RecoveryResult.Restored -> {
                Log.d(TAG, "Restoring active session ${result.session.sessionId}...")
                _sessionState.value = FocusSessionState.RECOVERING
                _activeSession.value = result.session
                _remainingSeconds.value = result.remainingSeconds

                // Restore session snapshot
                BlockingController.createSessionSnapshot(
                    result.session.restrictedPackagesSnapshot,
                    result.session.isStrictMode
                )

                // Arm protection engine
                startInternalEngine(context, result.session)
                _sessionState.value = FocusSessionState.ACTIVE

                _telemetry.value = _telemetry.value.copy(
                    successfulRecoveries = _telemetry.value.successfulRecoveries + 1
                )
            }
            is SessionRecovery.RecoveryResult.Expired -> {
                Log.d(TAG, "Persisted session expired while process was down. Cleaned up.")
                _sessionState.value = FocusSessionState.COMPLETED
                _activeSession.value = null
                _remainingSeconds.value = 0
                BlockingController.clearSessionSnapshot()
                OverlayController.dismissOverlay()
            }
            is SessionRecovery.RecoveryResult.NoActiveSession -> {
                _sessionState.value = FocusSessionState.IDLE
                _activeSession.value = null
                _remainingSeconds.value = 0
            }
        }
    }

    /**
     * Starts a new Focus Protection Session.
     */
    @Synchronized
    fun startFocusSession(
        context: Context,
        subject: String,
        topic: String,
        durationMinutes: Int,
        isStrictMode: Boolean = false,
        restrictedPackages: Set<String> = emptySet(),
        isAutoStarted: Boolean = false,
        examName: String = "",
        sessionGoal: String = "",
        planItemId: Long? = null
    ): Boolean {
        Log.d(TAG, "Starting Focus Session: $subject - $topic (${durationMinutes}m, Strict=$isStrictMode)")
        _sessionState.value = FocusSessionState.STARTING
        _lastError.value = null

        // 1. Verify Permission Health
        val health = refreshHealthStatus(context)
        if (!health.isReady) {
            val error = FocusProtectionError(
                code = if (health.usageAccessStatus != PermissionCheckStatus.READY)
                    FocusProtectionErrorCode.USAGE_ACCESS_UNAVAILABLE
                else FocusProtectionErrorCode.OVERLAY_UNAVAILABLE,
                userFriendlyMessage = health.diagnosticMessage
            )
            _lastError.value = error
            _sessionState.value = FocusProtectionErrorStateOrIdle(error)
            Log.w(TAG, "Cannot start focus session: ${error.userFriendlyMessage}")
            return false
        }

        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val totalSecs = durationMinutes * 60

        val session = PersistedFocusSession(
            sessionId = sessionId,
            subject = subject.ifBlank { "Study Session" },
            topic = topic.ifBlank { "Deep Focus" },
            examName = examName,
            sessionGoal = sessionGoal,
            planItemId = planItemId,
            plannedDurationMinutes = durationMinutes,
            startedAtTimestamp = now,
            elapsedRealtimeStart = android.os.SystemClock.elapsedRealtime(),
            pausedAccumulatedSeconds = 0L,
            pauseStartTimestamp = 0L,
            isPaused = false,
            isStrictMode = isStrictMode,
            isAutoStarted = isAutoStarted,
            state = FocusSessionExecutionState.FOCUS_ACTIVE,
            restrictedPackagesSnapshot = restrictedPackages
        )

        // 2. Persist Session immediately
        FocusSessionPersistence.getInstance(context).saveActiveSession(session)
        _activeSession.value = session
        _remainingSeconds.value = totalSecs

        // 3. Create Session Snapshot
        BlockingController.createSessionSnapshot(restrictedPackages, isStrictMode)

        // 4. Start Internal Monitoring Engine
        startInternalEngine(context, session)

        _sessionState.value = FocusSessionState.ACTIVE
        _telemetry.value = _telemetry.value.copy(
            totalProtectionStarts = _telemetry.value.totalProtectionStarts + 1
        )

        return true
    }

    private fun FocusProtectionErrorStateOrIdle(error: FocusProtectionError): FocusSessionState {
        return FocusSessionState.ERROR
    }

    private fun startInternalEngine(context: Context, session: PersistedFocusSession) {
        val appContext = context.applicationContext
        this.appContext = appContext

        // Start Usage Monitor
        usageMonitor?.stopMonitoring()
        usageMonitor = UsageMonitor(
            context = appContext,
            onPackageDetected = { detectedPackage ->
                handleForegroundPackage(appContext, detectedPackage)
            },
            onError = { error ->
                Log.e(TAG, "UsageMonitor error: ${error.userFriendlyMessage}")
                _lastError.value = error
            }
        ).also { it.startMonitoring() }

        // Start Timestamp-based Ticker Job
        startSessionTicker(context, session)
    }

    private fun startSessionTicker(context: Context, session: PersistedFocusSession) {
        tickerJob?.cancel()
        tickerJob = engineScope.launch {
            while (isActive && _sessionState.value == FocusSessionState.ACTIVE) {
                val current = _activeSession.value ?: break
                val remaining = current.calculateRemainingSeconds()
                _remainingSeconds.value = remaining

                if (remaining <= 0) {
                    Log.d(TAG, "Focus Session timer reached zero. Concluding session.")
                    withContext(Dispatchers.Main) {
                        completeFocusSession(context)
                    }
                    break
                }
                delay(1000L)
            }
        }
    }

    /**
     * Deterministic Foreground Package Evaluation
     */
    private fun handleForegroundPackage(context: Context, packageName: String) {
        val currentSession = _activeSession.value ?: return
        if (_sessionState.value != FocusSessionState.ACTIVE) return

        val decision = BlockingController.evaluateBlockDecision(
            packageName = packageName,
            currentPackage = context.packageName,
            sessionState = _sessionState.value
        )

        when (decision) {
            BlockDecision.BLOCK -> {
                Log.d(TAG, "Deterministic decision: BLOCK package $packageName")
                val presented = OverlayController.presentBlockOverlay(
                    context = context,
                    packageName = packageName,
                    sessionId = currentSession.sessionId
                )
                if (presented) {
                    _telemetry.value = _telemetry.value.copy(
                        totalInterceptions = _telemetry.value.totalInterceptions + 1,
                        lastInterceptionTimestamp = System.currentTimeMillis()
                    )
                }
            }
            BlockDecision.ALLOW, BlockDecision.SYSTEM_EXCEPTION -> {
                OverlayController.onAllowedAppDetected(packageName)
            }
            BlockDecision.PROTECTION_UNAVAILABLE -> {
                Log.w(TAG, "Protection unavailable for $packageName")
            }
        }
    }

    /**
     * Pauses the active focus session.
     */
    @Synchronized
    fun pauseFocusSession(context: Context) {
        val current = _activeSession.value ?: return
        if (current.isStrictMode) {
            Log.w(TAG, "Pause requested but Strict Mode is ACTIVE. Denied.")
            return
        }
        _sessionState.value = FocusSessionState.PAUSED
        FocusSessionPersistence.getInstance(context).updatePauseState(true)
        usageMonitor?.stopMonitoring()
    }

    /**
     * Resumes the paused focus session.
     */
    @Synchronized
    fun resumeFocusSession(context: Context) {
        val current = _activeSession.value ?: return
        _sessionState.value = FocusSessionState.ACTIVE
        FocusSessionPersistence.getInstance(context).updatePauseState(false)
        usageMonitor?.startMonitoring()
        startSessionTicker(context, current)
    }

    /**
     * Ends the focus session early (guarded by Strict Mode).
     */
    @Synchronized
    fun endFocusSession(context: Context, forceOverride: Boolean = false): Boolean {
        val current = _activeSession.value
        if (current != null && current.isStrictMode && !forceOverride) {
            Log.w(TAG, "End Focus requested but Strict Mode is ACTIVE. Denied.")
            return false
        }

        _sessionState.value = FocusSessionState.ENDING
        shutdownEngine(context)
        _sessionState.value = FocusSessionState.IDLE
        return true
    }

    /**
     * Natural timer completion. Strict Mode automatically resets to OFF.
     */
    @Synchronized
    fun completeFocusSession(context: Context) {
        _sessionState.value = FocusSessionState.ENDING
        shutdownEngine(context)
        _sessionState.value = FocusSessionState.COMPLETED
        Log.d(TAG, "Focus Session successfully completed. Strict mode automatically deactivated for future sessions.")
    }

    private fun shutdownEngine(context: Context) {
        tickerJob?.cancel()
        tickerJob = null

        usageMonitor?.stopMonitoring()
        usageMonitor = null

        BlockingController.clearSessionSnapshot()
        OverlayController.dismissOverlay()

        FocusSessionPersistence.getInstance(context).clearActiveSession()
        _activeSession.value = null
        _remainingSeconds.value = 0
    }

    fun isStrictActive(): Boolean {
        return _activeSession.value?.isStrictMode == true && _sessionState.value == FocusSessionState.ACTIVE
    }

    fun isSessionRunning(): Boolean {
        return _sessionState.value == FocusSessionState.ACTIVE
    }

    fun updateRemainingSeconds(seconds: Int) {
        _remainingSeconds.value = seconds
    }

    fun triggerInterruption(context: Context, blockedPkg: String) {
        val currentSession = _activeSession.value
        OverlayController.presentBlockOverlay(
            context = context,
            packageName = blockedPkg,
            sessionId = currentSession?.sessionId ?: ""
        )
    }
}
