package com.example.service.focus

/**
 * Focus Protection Engine 2.0 Core Models & State Machine definitions.
 */

enum class FocusSessionState {
    IDLE,
    STARTING,
    ACTIVE,
    PAUSED,
    ENDING,
    COMPLETED,
    RECOVERING,
    ERROR;

    val isActive: Boolean
        get() = this == ACTIVE || this == STARTING

    val isTerminated: Boolean
        get() = this == IDLE || this == COMPLETED || this == ERROR
}

enum class BlockDecision {
    BLOCK,
    ALLOW,
    SYSTEM_EXCEPTION,
    PROTECTION_UNAVAILABLE
}

enum class ProtectionStatusLevel {
    ACTIVE,
    LIMITED,
    ERROR
}

enum class PermissionCheckStatus {
    READY,
    ACTION_REQUIRED,
    OPTIONAL_RECOMMENDED,
    UNSUPPORTED
}

data class ProtectionHealthState(
    val isReady: Boolean,
    val usageAccessStatus: PermissionCheckStatus,
    val overlayStatus: PermissionCheckStatus,
    val backgroundStatus: PermissionCheckStatus,
    val overallStatus: ProtectionStatusLevel,
    val diagnosticMessage: String,
    val oemNotice: String? = null
)

data class FocusBlockItem(
    val packageName: String,
    val displayName: String,
    val category: String = "Apps",
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

enum class FocusProtectionErrorCode {
    USAGE_ACCESS_UNAVAILABLE,
    OVERLAY_UNAVAILABLE,
    BACKGROUND_RESTRICTION,
    MONITOR_START_FAILED,
    OVERLAY_START_FAILED,
    SESSION_EXPIRED,
    RECOVERY_FAILED,
    DEVICE_RESTRICTION
}

data class FocusProtectionError(
    val code: FocusProtectionErrorCode,
    val userFriendlyMessage: String,
    val technicalDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class FocusEngineTelemetry(
    val totalProtectionStarts: Long = 0L,
    val totalInterceptions: Long = 0L,
    val successfulRecoveries: Long = 0L,
    val lastInterceptionTimestamp: Long = 0L
)
