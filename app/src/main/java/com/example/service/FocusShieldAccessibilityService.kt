package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class FocusShieldAccessibilityService : AccessibilityService() {

    private var lastTriggeredPackage: String = ""
    private var lastTriggerTime: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Check if Focus Session is currently active
        if (!FocusShieldManager.isSessionActive.value) return

        // Never interfere with system apps or whitelist
        if (FocusShieldManager.ESSENTIAL_APPS_WHITELIST.contains(pkgName)) return
        if (pkgName == packageName) return

        // Check if user explicitly restricted this app
        if (FocusShieldManager.isAppRestricted(pkgName)) {
            val now = System.currentTimeMillis()
            // Debounce launches within 1.5 seconds for smooth UX
            if (pkgName == lastTriggeredPackage && (now - lastTriggerTime) < 1500L) {
                return
            }
            lastTriggeredPackage = pkgName
            lastTriggerTime = now

            // Launch Focus Shield Block Activity
            val intent = Intent(this, FocusShieldBlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("BLOCKED_PACKAGE", pkgName)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusShieldManager.init(applicationContext)
    }
}
