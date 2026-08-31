package com.example.service.enterprise

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Step 85: Enterprise Circuit Breaker Manager.
 * 
 * Provides fault tolerance for third-party and remote resources (AI APIs, WhatsApp Scrapers, Supabase, Remote URLs):
 * - Fast-fails calls when a downstream service is down (State: OPEN)
 * - Probes recovery after a configurable cooldown period (State: HALF_OPEN)
 * - Automatically resets upon successful execution (State: CLOSED)
 * - Triggers recovery notifications and incidents without alert storms
 */
class CircuitBreakerManager(
    private val failureThreshold: Int = 5,
    private val cooldownDurationMs: Long = 30000L,
    private val onStateChanged: ((service: String, oldState: CircuitState, newState: CircuitState) -> Unit)? = null
) {
    companion object {
        private const val TAG = "CircuitBreakerManager"
    }

    private data class ServiceCircuit(
        var state: CircuitState = CircuitState.CLOSED,
        var consecutiveFailures: Int = 0,
        var lastFailureTime: Long = 0L,
        var lastStateChangeTime: Long = System.currentTimeMillis()
    )

    private val circuits = ConcurrentHashMap<String, ServiceCircuit>()

    /**
     * Checks if a request to the target service is permitted.
     */
    @Synchronized
    fun allowRequest(serviceKey: String): Boolean {
        val circuit = circuits.getOrPut(serviceKey) { ServiceCircuit() }
        val now = System.currentTimeMillis()

        return when (circuit.state) {
            CircuitState.CLOSED -> true
            CircuitState.OPEN -> {
                if (now - circuit.lastFailureTime > cooldownDurationMs) {
                    val oldState = circuit.state
                    circuit.state = CircuitState.HALF_OPEN
                    circuit.lastStateChangeTime = now
                    Log.i(TAG, "Circuit for [$serviceKey] transitioning from OPEN to HALF_OPEN (cooldown elapsed)")
                    onStateChanged?.invoke(serviceKey, oldState, CircuitState.HALF_OPEN)
                    true // Allow single probe request
                } else {
                    false // Circuit is open, fast-fail
                }
            }
            CircuitState.HALF_OPEN -> true // Allow probe
        }
    }

    /**
     * Records a successful execution against the service.
     */
    @Synchronized
    fun recordSuccess(serviceKey: String) {
        val circuit = circuits.getOrPut(serviceKey) { ServiceCircuit() }
        val oldState = circuit.state

        if (oldState != CircuitState.CLOSED) {
            Log.i(TAG, "Circuit for [$serviceKey] RECOVERED! Transitioning from $oldState to CLOSED.")
            circuit.state = CircuitState.CLOSED
            circuit.consecutiveFailures = 0
            circuit.lastStateChangeTime = System.currentTimeMillis()
            onStateChanged?.invoke(serviceKey, oldState, CircuitState.CLOSED)
        } else {
            circuit.consecutiveFailures = 0
        }
    }

    /**
     * Records a failure for the service.
     */
    @Synchronized
    fun recordFailure(serviceKey: String): CircuitState {
        val circuit = circuits.getOrPut(serviceKey) { ServiceCircuit() }
        val oldState = circuit.state
        val now = System.currentTimeMillis()

        circuit.consecutiveFailures++
        circuit.lastFailureTime = now

        if (oldState == CircuitState.HALF_OPEN) {
            // Probe failed, immediately trip back to OPEN
            circuit.state = CircuitState.OPEN
            circuit.lastStateChangeTime = now
            Log.w(TAG, "Circuit probe for [$serviceKey] failed! Transitioning back to OPEN.")
            onStateChanged?.invoke(serviceKey, oldState, CircuitState.OPEN)
        } else if (circuit.consecutiveFailures >= failureThreshold && oldState == CircuitState.CLOSED) {
            circuit.state = CircuitState.OPEN
            circuit.lastStateChangeTime = now
            Log.e(TAG, "Circuit for [$serviceKey] TRIPPED to OPEN after ${circuit.consecutiveFailures} consecutive failures.")
            onStateChanged?.invoke(serviceKey, oldState, CircuitState.OPEN)
        }

        return circuit.state
    }

    fun getState(serviceKey: String): CircuitState {
        val circuit = circuits[serviceKey] ?: return CircuitState.CLOSED
        val now = System.currentTimeMillis()
        if (circuit.state == CircuitState.OPEN && (now - circuit.lastFailureTime > cooldownDurationMs)) {
            return CircuitState.HALF_OPEN
        }
        return circuit.state
    }

    fun getConsecutiveFailures(serviceKey: String): Int {
        return circuits[serviceKey]?.consecutiveFailures ?: 0
    }

    fun resetAll() {
        circuits.clear()
    }
}
