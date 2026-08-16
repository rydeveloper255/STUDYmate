package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class FocusShieldAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Check if Focus Session is currently active and shield is enabled
        if (!FocusShieldManager.isSessionActive.value || !FocusShieldManager.isShieldEnabled()) return

        // Never interfere with StudyMate AI or system essential apps
        if (pkgName == packageName) return
        if (FocusShieldManager.ESSENTIAL_APPS_WHITELIST.contains(pkgName)) return

        // Check if user explicitly restricted this app
        if (FocusShieldManager.isAppRestricted(pkgName)) {
            FocusShieldManager.triggerInterruption(this, pkgName)
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
