package com.example.data.remote.supabase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe client-side request deduplication and rate limiter.
 * Protects against double-taps on expensive operations (NOVA queries, test submissions, PDF generation).
 */
object RequestRateLimiter {
    private val lastRequestTimestamps = ConcurrentHashMap<String, Long>()
    private val inFlightOperations = ConcurrentHashMap<String, Boolean>()
    private val mutex = Mutex()

    /**
     * Checks if an action key can be executed.
     * Enforces minimum debounce interval (e.g. 800ms) and in-flight deduplication.
     */
    suspend fun canExecute(key: String, minIntervalMs: Long = 800L): Boolean = mutex.withLock {
        if (inFlightOperations[key] == true) {
            return false // Already running
        }
        val now = System.currentTimeMillis()
        val lastTime = lastRequestTimestamps[key] ?: 0L
        if (now - lastTime < minIntervalMs) {
            return false // Debounced
        }
        lastRequestTimestamps[key] = now
        inFlightOperations[key] = true
        return true
    }

    /**
     * Releases the in-flight lock for an action key once the operation finishes.
     */
    suspend fun release(key: String) = mutex.withLock {
        inFlightOperations.remove(key)
    }

    /**
     * Executes an operation with automatic deduplication lock and release.
     */
    suspend fun <T> executeSafely(
        key: String,
        minIntervalMs: Long = 800L,
        onThrottled: (suspend () -> T)? = null,
        block: suspend () -> T
    ): T? {
        if (!canExecute(key, minIntervalMs)) {
            return onThrottled?.invoke()
        }
        return try {
            block()
        } finally {
            release(key)
        }
    }
}

/**
 * Generates and tracks unique idempotency keys for atomic operations.
 */
object IdempotencyHelper {
    fun generateOperationKey(prefix: String, identifier: String): String {
        return "${prefix}_${identifier}_${System.currentTimeMillis()}"
    }

    fun generateSessionKey(userId: String, testId: String): String {
        return "test_session_${userId.take(8)}_${testId}"
    }
}
