package com.example.service.focus

import android.content.Context
import android.util.Log
import com.example.service.FocusSessionExecutionState
import com.example.service.FocusSessionPersistence
import com.example.service.PersistedFocusSession

/**
 * Focus Protection Engine 2.0 - Session Recovery Subsystem
 * Restores active focus sessions following process termination or reboot
 * using immutable timestamp comparisons.
 */
class SessionRecovery(private val context: Context) {

    companion object {
        private const val TAG = "SessionRecovery"
    }

    sealed class RecoveryResult {
        data class Restored(val session: PersistedFocusSession, val remainingSeconds: Int) : RecoveryResult()
        data class Expired(val completedSession: PersistedFocusSession) : RecoveryResult()
        object NoActiveSession : RecoveryResult()
    }

    /**
     * Inspects stored session records to deterministically recover active state or mark completion.
     */
    fun evaluateRecovery(): RecoveryResult {
        val persistence = FocusSessionPersistence.getInstance(context)
        val session = persistence.loadActiveSession() ?: return RecoveryResult.NoActiveSession

        if (session.state == FocusSessionExecutionState.FOCUS_COMPLETED ||
            session.state == FocusSessionExecutionState.INTERRUPTED ||
            session.state == FocusSessionExecutionState.IDLE
        ) {
            return RecoveryResult.NoActiveSession
        }

        val remainingSecs = session.calculateRemainingSeconds()
        return if (remainingSecs <= 0) {
            Log.d(TAG, "Session ${session.sessionId} expired during background/process death. Marking completed.")
            persistence.saveActiveSession(session.copy(state = FocusSessionExecutionState.FOCUS_COMPLETED))
            RecoveryResult.Expired(session)
        } else {
            Log.d(TAG, "Session ${session.sessionId} is still active with $remainingSecs seconds remaining. Restoring.")
            RecoveryResult.Restored(session, remainingSecs)
        }
    }
}
