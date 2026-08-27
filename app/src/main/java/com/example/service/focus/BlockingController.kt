package com.example.service.focus

import android.content.Context
import android.util.Log

/**
 * Focus Protection Engine 2.0 - Blocking Decision Controller
 * Houses the deterministic decision engine and immutable session snapshot logic.
 */
object BlockingController {

    private const val TAG = "BlockingController"

    // Critical Android system packages that must never be interrupted
    val SYSTEM_ALLOWLIST = setOf(
        "android",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.incallui",
        "com.android.emergency",
        "com.google.android.apps.safetyhub",
        "com.android.settings",
        "com.google.android.settings",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin", // Gboard
        "com.samsung.android.honeyboard",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    // Trusted Financial / Utility Apps that run with zero interruption by default
    val FINANCIAL_AND_UTILITY_EXCEPTIONS = setOf(
        "net.one97.paytm",
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "in.org.npci.upiapp",
        "com.sbi.lotusintouch",
        "com.msf.kbank.mobile",
        "com.icicibank.mobile",
        "com.axis.mobile",
        "com.bankofbaroda.mconnect",
        "com.csam.icici.bank.imobile"
    )

    // Snapshot of restricted package identifiers locked at session start
    @Volatile
    private var sessionSnapshotPackages: Set<String> = emptySet()

    @Volatile
    private var isSnapshotLockedByStrictMode: Boolean = false

    /**
     * Initializes a snapshot of restricted packages for a newly started focus session.
     */
    fun createSessionSnapshot(packages: Set<String>, isStrictMode: Boolean) {
        sessionSnapshotPackages = HashSet(packages)
        isSnapshotLockedByStrictMode = isStrictMode
        Log.d(TAG, "Created session snapshot with ${packages.size} packages. StrictMode=$isStrictMode")
    }

    /**
     * Allows updating block list during active session ONLY if Strict Mode is OFF.
     */
    fun updateSessionSnapshotIfAllowed(packages: Set<String>): Boolean {
        if (isSnapshotLockedByStrictMode) {
            Log.w(TAG, "Attempted to modify block list while Strict Mode is ACTIVE. Denied.")
            return false
        }
        sessionSnapshotPackages = HashSet(packages)
        return true
    }

    /**
     * Clears the active snapshot on session completion.
     */
    fun clearSessionSnapshot() {
        sessionSnapshotPackages = emptySet()
        isSnapshotLockedByStrictMode = false
    }

    fun getActiveSnapshot(): Set<String> = sessionSnapshotPackages

    /**
     * Authoritative Deterministic Block Decision Function
     *
     * @param packageName The detected foreground package identifier.
     * @param currentPackage StudyMate's own package name.
     * @param sessionState Current state in the Focus State Machine.
     * @param customExceptions Any user-configured permanent exceptions.
     * @return Deterministic [BlockDecision]
     */
    fun evaluateBlockDecision(
        packageName: String,
        currentPackage: String,
        sessionState: FocusSessionState,
        customExceptions: Set<String> = emptySet()
    ): BlockDecision {
        if (packageName.isBlank()) {
            return BlockDecision.ALLOW
        }

        // 1. Never block StudyMate itself
        if (packageName == currentPackage) {
            return BlockDecision.ALLOW
        }

        // 2. Never block if session is not actively enforcing
        if (sessionState != FocusSessionState.ACTIVE) {
            return BlockDecision.ALLOW
        }

        // 3. Android Core System Packages & Launchers Protection
        if (SYSTEM_ALLOWLIST.contains(packageName) || packageName.startsWith("com.android.systemui")) {
            return BlockDecision.SYSTEM_EXCEPTION
        }

        // 4. User Permanent Custom Exceptions
        if (customExceptions.contains(packageName)) {
            return BlockDecision.ALLOW
        }

        // 5. Financial & Utility Protection Check:
        // If it's a financial app and NOT explicitly placed in session block list, allow safely.
        if (FINANCIAL_AND_UTILITY_EXCEPTIONS.contains(packageName) && !sessionSnapshotPackages.contains(packageName)) {
            return BlockDecision.ALLOW
        }

        // 6. Check against active session snapshot
        return if (sessionSnapshotPackages.contains(packageName)) {
            BlockDecision.BLOCK
        } else {
            BlockDecision.ALLOW
        }
    }
}
