package com.example.service.collector

import android.content.Context
import android.util.Log
import com.example.data.model.content.ContentCollectionJobLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * AutomatedContentScheduler (Step 63 Implementation)
 * 
 * Manages the background 3-hour automated cycle for Content Collection & Telegram Bot dispatch.
 * Fully configurable intervals (60m, 180m, 360m, 720m, 1440m), manual trigger, and real-time status reporting.
 */
class AutomatedContentScheduler(
    private val context: Context,
    private val collectorEngine: AutomatedContentCollectorEngine,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    companion object {
        private const val TAG = "ContentScheduler"
        
        // 3-Hour Configurable Interval (Default: 180 minutes)
        const val DEFAULT_INTERVAL_MINUTES: Long = 180L
        val DEFAULT_INTERVAL_MILLIS: Long = TimeUnit.MINUTES.toMillis(DEFAULT_INTERVAL_MINUTES)
    }

    private var intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES
    private var intervalMillis: Long = DEFAULT_INTERVAL_MILLIS
    private var schedulerJob: Job? = null

    // Scheduler Observable States
    private val _isSchedulerRunning = MutableStateFlow(false)
    val isSchedulerRunning: StateFlow<Boolean> = _isSchedulerRunning.asStateFlow()

    private val _lastRunTimestamp = MutableStateFlow<Long?>(null)
    val lastRunTimestamp: StateFlow<Long?> = _lastRunTimestamp.asStateFlow()

    private val _nextRunTimestamp = MutableStateFlow<Long?>(null)
    val nextRunTimestamp: StateFlow<Long?> = _nextRunTimestamp.asStateFlow()

    private val _executionCount = MutableStateFlow(0)
    val executionCount: StateFlow<Int> = _executionCount.asStateFlow()

    private val _currentIntervalMinutes = MutableStateFlow(DEFAULT_INTERVAL_MINUTES)
    val currentIntervalMinutes: StateFlow<Long> = _currentIntervalMinutes.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<ContentCollectionJobLog>>(emptyList())
    val recentLogs: StateFlow<List<ContentCollectionJobLog>> = _recentLogs.asStateFlow()

    init {
        // Start scheduler on initialization
        startScheduler()
    }

    /**
     * Starts the automated periodic collection scheduler.
     */
    fun startScheduler() {
        if (schedulerJob?.isActive == true) {
            Log.d(TAG, "Scheduler is already active.")
            return
        }

        _isSchedulerRunning.value = true
        _nextRunTimestamp.value = System.currentTimeMillis() + intervalMillis

        schedulerJob = coroutineScope.launch {
            Log.i(TAG, "Automated content collection scheduler started. Interval: ${intervalMinutes}m")

            // Immediate initial execution on launch
            executeCycleInternal()

            while (isActive && _isSchedulerRunning.value) {
                val nextRun = System.currentTimeMillis() + intervalMillis
                _nextRunTimestamp.value = nextRun

                delay(intervalMillis)

                if (isActive && _isSchedulerRunning.value) {
                    executeCycleInternal()
                }
            }
        }
    }

    /**
     * Pauses or stops the periodic scheduler.
     */
    fun stopScheduler() {
        _isSchedulerRunning.value = false
        schedulerJob?.cancel()
        schedulerJob = null
        _nextRunTimestamp.value = null
        Log.i(TAG, "Automated content collection scheduler stopped.")
    }

    /**
     * Configures the scheduling interval in minutes (e.g. 60, 180, 360, 720, 1440).
     */
    fun updateIntervalMinutes(minutes: Long) {
        val safeMinutes = minutes.coerceAtLeast(30L)
        intervalMinutes = safeMinutes
        intervalMillis = TimeUnit.MINUTES.toMillis(safeMinutes)
        _currentIntervalMinutes.value = safeMinutes
        Log.i(TAG, "Scheduler interval updated to $safeMinutes minutes")
        if (_isSchedulerRunning.value) {
            stopScheduler()
            startScheduler()
        }
    }

    /**
     * Triggers a manual collection run on-demand (e.g. "Check Sources Now" button).
     */
    suspend fun triggerManualRun(): ContentCollectionJobLog {
        Log.i(TAG, "Manual content collection triggered by developer/admin.")
        return executeCycleInternal()
    }

    private suspend fun executeCycleInternal(): ContentCollectionJobLog {
        val now = System.currentTimeMillis()
        _lastRunTimestamp.value = now

        val jobLog = try {
            collectorEngine.executeCollectionCycle()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during scheduler run: ${e.message}", e)
            ContentCollectionJobLog(
                startedAt = now,
                completedAt = System.currentTimeMillis(),
                status = "FAILED",
                sourcesChecked = 0,
                newItems = 0,
                duplicateItems = 0,
                failedSources = 1,
                telegramPosts = 0,
                pdfsDetected = 0,
                errors = listOf("Scheduler cycle error: ${e.message}")
            )
        }

        _executionCount.value += 1
        _recentLogs.value = (listOf(jobLog) + _recentLogs.value).take(20)
        _nextRunTimestamp.value = System.currentTimeMillis() + intervalMillis

        return jobLog
    }

    fun getFormattedNextRun(): String {
        val next = _nextRunTimestamp.value ?: return "Not scheduled"
        return SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US).format(Date(next))
    }

    fun getFormattedLastRun(): String {
        val last = _lastRunTimestamp.value ?: return "Never"
        return SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US).format(Date(last))
    }
}
