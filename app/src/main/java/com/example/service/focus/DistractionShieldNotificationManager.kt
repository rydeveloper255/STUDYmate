package com.example.service.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * StudyMate AI — Native Android Distraction Shield & Notification Policy Interruption Controller
 *
 * Uses official Android NotificationManager APIs:
 * - NotificationManager.isNotificationPolicyAccessGranted
 * - NotificationManager.getCurrentInterruptionFilter / setInterruptionFilter
 * - Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
 *
 * Guarantees:
 * - Captures exact pre-focus interruption filter
 * - Activates PRIORITY interruption filter (reducing non-critical notification noise)
 * - Restores exact pre-focus interruption filter idempotently on focus completion/stop
 * - App restart / crash recovery safety
 */
object DistractionShieldNotificationManager {

    private const val TAG = "DistractionShieldDnd"
    private const val PREFS_NAME = "studymate_dnd_shield_prefs"
    private const val KEY_PREVIOUS_FILTER = "previous_interruption_filter"
    private const val KEY_IS_SHIELD_ACTIVE = "is_shield_dnd_active"

    private var previousFilterSnapshot: Int = NotificationManager.INTERRUPTION_FILTER_ALL
    private var isDndShieldActive: Boolean = false

    /**
     * Checks whether the app has Notification Policy Access (DND) permission
     */
    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        return try {
            notificationManager.isNotificationPolicyAccessGranted
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification policy access", e)
            false
        }
    }

    /**
     * Opens Android System Settings for Notification Policy Access
     */
    fun openNotificationPolicySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification policy settings", e)
            }
        }
    }

    /**
     * Activates DND (Priority Interruption Filter) safely:
     * 1. Captures current system interruption filter
     * 2. Sets filter to INTERRUPTION_FILTER_PRIORITY
     * 3. Saves state to SharedPreferences for crash/kill safety
     */
    @Synchronized
    fun activateDistractionShield(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "Notification policy access not granted; cannot activate DND")
            return false
        }

        if (isDndShieldActive) {
            Log.d(TAG, "Distraction Shield DND already active; skipping duplicate activation")
            return true
        }

        try {
            // 1. Capture current system filter
            val currentFilter = notificationManager.currentInterruptionFilter
            previousFilterSnapshot = currentFilter

            // 2. Persist to SharedPreferences for process death safety
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_PREVIOUS_FILTER, currentFilter)
                .putBoolean(KEY_IS_SHIELD_ACTIVE, true)
                .apply()

            // 3. Set to PRIORITY filter (quiets non-priority notifications while respecting emergency policies)
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            isDndShieldActive = true
            Log.d(TAG, "Distraction Shield DND activated (Previous filter: $currentFilter -> Priority Filter)")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while activating interruption filter", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception while activating interruption filter", e)
            return false
        }
    }

    /**
     * Deactivates DND and restores the previous system state idempotently
     */
    @Synchronized
    fun restorePreviousDndState(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "Notification policy access not granted; cannot restore DND")
            return false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val filterToRestore = if (isDndShieldActive) {
            previousFilterSnapshot
        } else {
            prefs.getInt(KEY_PREVIOUS_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        try {
            notificationManager.setInterruptionFilter(filterToRestore)
            isDndShieldActive = false
            prefs.edit()
                .putBoolean(KEY_IS_SHIELD_ACTIVE, false)
                .apply()
            Log.d(TAG, "Distraction Shield DND deactivated. Restored filter: $filterToRestore")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while restoring interruption filter", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception while restoring interruption filter", e)
            return false
        }
    }

    /**
     * App Startup / Crash Recovery:
     * If app died while Distraction Shield was active, restore previous DND state immediately
     */
    fun recoverDndStateIfLingering(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wasActive = prefs.getBoolean(KEY_IS_SHIELD_ACTIVE, false)
        if (wasActive) {
            Log.d(TAG, "Detected lingering Distraction Shield state after app restart. Restoring previous DND state...")
            restorePreviousDndState(context)
        }
    }
}
