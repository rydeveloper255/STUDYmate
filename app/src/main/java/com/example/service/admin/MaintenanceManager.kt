package com.example.service.admin

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages StudyMate System Maintenance Mode.
 *
 * When Maintenance Mode is active:
 * - App continues to support offline study & local notes/practice.
 * - Displays a non-intrusive, clear status indicator and helpful maintenance guidance.
 * - Admin can toggle dynamically via Telegram `/maintenance on` and `/maintenance off`.
 */
object MaintenanceManager {
    private const val PREFS_NAME = "studymate_admin_telegram_prefs"
    private const val KEY_MAINTENANCE = "maintenance_mode_active"
    private const val KEY_MAINTENANCE_SINCE = "maintenance_mode_since"
    private const val KEY_MAINTENANCE_REASON = "maintenance_mode_reason"

    private val _isMaintenanceActive = MutableStateFlow(false)
    val isMaintenanceActive: StateFlow<Boolean> = _isMaintenanceActive.asStateFlow()

    private val _maintenanceReason = MutableStateFlow("Scheduled System Optimization & Security Upgrade")
    val maintenanceReason: StateFlow<String> = _maintenanceReason.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isMaintenanceActive.value = prefs.getBoolean(KEY_MAINTENANCE, false)
        _maintenanceReason.value = prefs.getString(KEY_MAINTENANCE_REASON, "Scheduled System Optimization & Security Upgrade") ?: "Scheduled System Optimization"
    }

    fun setMaintenanceMode(active: Boolean, reason: String = "Scheduled System Optimization & Security Upgrade", context: Context? = null): Boolean {
        _isMaintenanceActive.value = active
        _maintenanceReason.value = reason
        val ctx = context ?: appContext
        if (ctx != null) {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MAINTENANCE, active)
                .putString(KEY_MAINTENANCE_SINCE, if (active) now else "")
                .putString(KEY_MAINTENANCE_REASON, reason)
                .apply()
        }
        return active
    }

    fun getMaintenanceInfo(context: Context? = null): String {
        val ctx = context ?: appContext ?: return if (_isMaintenanceActive.value) "ACTIVE" else "OFF"
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = prefs.getBoolean(KEY_MAINTENANCE, false)
        val since = prefs.getString(KEY_MAINTENANCE_SINCE, "N/A")
        val reason = prefs.getString(KEY_MAINTENANCE_REASON, "Routine Maintenance")
        return if (active) {
            "ACTIVE (Since: $since | Reason: $reason)"
        } else {
            "OFF (All live cloud & AI features normal)"
        }
    }
}
