package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusShieldAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val pkgName = event.packageName?.toString() ?: return

            // Notify Safety Layer about foreground app change
            AccessibilitySafetyManager.onPackageForegroundChanged(pkgName)

            // Never interfere with StudyMate AI itself
            if (pkgName == packageName) return

            // If accessibility is paused by user or app is sensitive (Banking/Payments/Auth), DO NOTHING
            if (AccessibilitySafetyManager.shouldSuppressInterruption(pkgName)) return

            // Check if Focus Session is currently active and shield is enabled
            if (!FocusShieldManager.isSessionActive.value || !FocusShieldManager.isShieldEnabled()) return

            // Never interfere with system essential apps
            if (FocusShieldManager.ESSENTIAL_APPS_WHITELIST.contains(pkgName)) return

            // Check if user explicitly restricted this distracting app (e.g., YouTube, Instagram)
            if (FocusShieldManager.isAppRestricted(pkgName)) {
                FocusShieldManager.triggerInterruption(this, pkgName)
            }
        } catch (t: Throwable) {
            // Guarantee 100% crash recovery — service failures will never crash app or device
            Log.e("AccessibilityService", "Safe event processing error: ${t.message}", t)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            FocusShieldManager.init(applicationContext)
            AccessibilitySafetyManager.init(applicationContext)
        } catch (t: Throwable) {
            Log.e("AccessibilityService", "Error during service connection init: ${t.message}", t)
        }
    }
}

