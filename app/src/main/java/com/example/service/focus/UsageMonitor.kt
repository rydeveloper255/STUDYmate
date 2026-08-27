package com.example.service.focus

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Focus Protection Engine 2.0 - Usage Monitor
 * High-efficiency foreground application detector using UsageEvents & UsageStatsManager.
 * Employs adaptive intervals to conserve battery and avoid tight loops.
 */
class UsageMonitor(
    private val context: Context,
    private val onPackageDetected: (packageName: String) -> Unit,
    private val onError: (error: FocusProtectionError) -> Unit
) {

    companion object {
        private const val TAG = "UsageMonitor"
        private const val INTERVAL_ACTIVE_SCREEN_ON_MS = 600L
        private const val INTERVAL_ACTIVE_SCREEN_OFF_MS = 3000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private var lastObservedPackage: String? = null
    private var lastEventTimestamp: Long = 0L
    private var retryCount: Int = 0

    // Lightweight heartbeat state
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _lastHeartbeatTimestamp = MutableStateFlow(0L)
    val lastHeartbeatTimestamp: StateFlow<Long> = _lastHeartbeatTimestamp.asStateFlow()

    private val _lastDetectedPackage = MutableStateFlow<String?>(null)
    val lastDetectedPackage: StateFlow<String?> = _lastDetectedPackage.asStateFlow()

    /**
     * Starts adaptive foreground monitoring.
     */
    @Synchronized
    fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        if (usageStatsManager == null) {
            onError(
                FocusProtectionError(
                    code = FocusProtectionErrorCode.USAGE_ACCESS_UNAVAILABLE,
                    userFriendlyMessage = "UsageStatsManager system service is unavailable on this device."
                )
            )
            return
        }

        _isMonitoring.value = true
        retryCount = 0
        lastEventTimestamp = System.currentTimeMillis() - 5000L

        monitorJob = scope.launch {
            Log.d(TAG, "UsageMonitor started active foreground monitoring loop.")
            while (isActive && _isMonitoring.value) {
                try {
                    _lastHeartbeatTimestamp.value = System.currentTimeMillis()

                    val isInteractive = powerManager?.isInteractive ?: true
                    val currentPackage = detectForegroundPackage()

                    if (!currentPackage.isNullOrBlank()) {
                        _lastDetectedPackage.value = currentPackage
                        if (currentPackage != lastObservedPackage) {
                            lastObservedPackage = currentPackage
                            withContext(Dispatchers.Main) {
                                onPackageDetected(currentPackage)
                            }
                        }
                    }

                    // Reset retry count on successful iteration
                    retryCount = 0

                    val delayMs = if (isInteractive) INTERVAL_ACTIVE_SCREEN_ON_MS else INTERVAL_ACTIVE_SCREEN_OFF_MS
                    delay(delayMs)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during foreground package detection: ${e.message}")
                    retryCount++
                    if (retryCount >= MAX_RETRY_ATTEMPTS) {
                        _isMonitoring.value = false
                        withContext(Dispatchers.Main) {
                            onError(
                                FocusProtectionError(
                                    code = FocusProtectionErrorCode.MONITOR_START_FAILED,
                                    userFriendlyMessage = "Foreground monitoring failed repeatedly.",
                                    technicalDetails = e.message
                                )
                            )
                        }
                        break
                    }
                    // Exponential backoff
                    delay(1000L * (1 shl retryCount))
                }
            }
            _isMonitoring.value = false
            Log.d(TAG, "UsageMonitor loop ended.")
        }
    }

    /**
     * Uses UsageEvents (most accurate on modern Android) with fallback to queryUsageStats.
     */
    private fun detectForegroundPackage(): String? {
        val usm = usageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val beginTime = (endTime - 8000L).coerceAtLeast(lastEventTimestamp)

        try {
            val events = usm.queryEvents(beginTime, endTime)
            var latestPackage: String? = null
            var latestTime: Long = 0L

            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                // ACTIVITY_RESUMED (1) or ACTIVITY_STOPPED (2)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == 1 /* ACTIVITY_RESUMED constant */ ||
                    event.eventType == UsageEvents.Event.USER_INTERACTION
                ) {
                    if (event.timeStamp >= latestTime) {
                        latestPackage = event.packageName
                        latestTime = event.timeStamp
                    }
                }
            }

            if (!latestPackage.isNullOrBlank()) {
                lastEventTimestamp = latestTime
                return latestPackage
            }
        } catch (e: Exception) {
            // Fallback to queryUsageStats
        }

        // Fallback strategy: queryUsageStats for top recent app
        try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, endTime - 10000L, endTime)
            if (!stats.isNullOrEmpty()) {
                val mostRecent = stats.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && mostRecent.lastTimeUsed > 0L) {
                    return mostRecent.packageName
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "queryUsageStats fallback error: ${e.message}")
        }

        return null
    }

    @Synchronized
    fun stopMonitoring() {
        _isMonitoring.value = false
        monitorJob?.cancel()
        monitorJob = null
        lastObservedPackage = null
        Log.d(TAG, "UsageMonitor stopped.")
    }
}
