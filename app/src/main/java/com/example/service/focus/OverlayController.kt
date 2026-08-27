package com.example.service.focus

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.FocusShieldBlockActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Focus Protection Engine 2.0 - Overlay Controller
 * Manages full-screen distraction interception safely, prevents duplicate overlays,
 * and seamlessly handles rapid app switching.
 */
object OverlayController {

    private const val TAG = "OverlayController"

    @Volatile
    private var activeBlockedPackage: String? = null

    @Volatile
    private var isOverlayVisible: Boolean = false

    @Volatile
    private var activeSessionId: String? = null

    @Volatile
    private var lastInterceptionTime: Long = 0L

    private val _currentBlockedPackageFlow = MutableStateFlow<String?>(null)
    val currentBlockedPackageFlow: StateFlow<String?> = _currentBlockedPackageFlow.asStateFlow()

    /**
     * Attempts to present the Focus Shield Block Activity overlay.
     * Guaranteed duplicate prevention: checks session id, package, and active visibility state.
     */
    @Synchronized
    fun presentBlockOverlay(
        context: Context,
        packageName: String,
        sessionId: String
    ): Boolean {
        val now = System.currentTimeMillis()

        // Prevent rapid thrashing of the same overlay within 1.5 seconds if already active
        if (isOverlayVisible && activeBlockedPackage == packageName && activeSessionId == sessionId) {
            return false
        }

        if (activeBlockedPackage == packageName && (now - lastInterceptionTime) < 1200L) {
            return false
        }

        activeBlockedPackage = packageName
        activeSessionId = sessionId
        isOverlayVisible = true
        lastInterceptionTime = now
        _currentBlockedPackageFlow.value = packageName

        return try {
            val intent = Intent(context, FocusShieldBlockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra("BLOCKED_PACKAGE", packageName)
                putExtra("SESSION_ID", sessionId)
            }
            context.startActivity(intent)
            Log.d(TAG, "Presented block overlay for $packageName in session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch block overlay: ${e.message}")
            isOverlayVisible = false
            false
        }
    }

    /**
     * Called when user transitions to an allowed app or back into StudyMate.
     */
    @Synchronized
    fun onAllowedAppDetected(packageName: String) {
        if (isOverlayVisible && activeBlockedPackage != null && activeBlockedPackage != packageName) {
            Log.d(TAG, "Transitioned from $activeBlockedPackage to allowed app $packageName. Clearing overlay state.")
            activeBlockedPackage = null
            isOverlayVisible = false
            _currentBlockedPackageFlow.value = null
        }
    }

    /**
     * Called when the block activity is dismissed or session is concluded.
     */
    @Synchronized
    fun dismissOverlay() {
        activeBlockedPackage = null
        isOverlayVisible = false
        _currentBlockedPackageFlow.value = null
    }

    fun isOverlayActive(): Boolean = isOverlayVisible
    fun getActiveBlockedPackage(): String? = activeBlockedPackage
}
