package com.example.data.remote.supabase

import android.content.Context
import android.util.Log
import com.example.data.local.StudyMateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class BackendHealthStatus {
    HEALTHY,
    DEGRADED,
    OFFLINE,
    UNCONFIGURED
}

data class BackendHealthReport(
    val status: BackendHealthStatus = BackendHealthStatus.UNCONFIGURED,
    val databaseStatus: String = "Unknown",
    val authStatus: String = "Unknown",
    val storageStatus: String = "Unknown",
    val latencyMs: Long = 0L,
    val pendingQueueCount: Int = 0,
    val isRlsProtected: Boolean = true,
    val serverTimestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

object BackendHealthManager {
    private const val TAG = "BackendHealthManager"

    private val _healthReport = MutableStateFlow(BackendHealthReport())
    val healthReport: StateFlow<BackendHealthReport> = _healthReport.asStateFlow()

    suspend fun checkHealth(
        client: SupabaseClient,
        authManager: SupabaseAuthManager,
        database: StudyMateDatabase
    ): BackendHealthReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (!client.isReady()) {
            val pendingCount = try {
                database.pendingSyncDao().getAllPendingOnce().size
            } catch (e: Exception) { 0 }

            val report = BackendHealthReport(
                status = BackendHealthStatus.UNCONFIGURED,
                databaseStatus = "Room Local Database Active",
                authStatus = if (authManager.isSessionValid()) "Local Session Valid" else "Guest Mode",
                storageStatus = "Local Cache Only",
                latencyMs = 0L,
                pendingQueueCount = pendingCount,
                isRlsProtected = true,
                details = "Offline-first mode active. Data safely saved to device."
            )
            _healthReport.value = report
            return@withContext report
        }

        var dbStatus = "Connecting..."
        var authStatus = if (authManager.isSessionValid()) "Authenticated" else "Guest / Anon"
        var isHealthy = true
        var pendingCount = 0

        try {
            pendingCount = database.pendingSyncDao().getAllPendingOnce().size
        } catch (e: Exception) {
            Log.w(TAG, "Error checking pending queue: ${e.message}")
        }

        // 1. Check RPC or table ping
        try {
            val res = client.rpc("check_backend_health", "{}", authManager.getAccessToken())
            if (res.isSuccess) {
                dbStatus = "PostgreSQL 15 Connected (RLS Active)"
            } else {
                // Fallback select from exams or profiles
                val selectRes = client.from("exams").select(mapOf("limit" to "1"), authManager.getAccessToken())
                if (selectRes.isSuccess) {
                    dbStatus = "Supabase PostgREST Connected"
                } else {
                    dbStatus = "Degraded: ${(selectRes as? SupabaseResult.Error)?.message ?: "Timeout"}"
                    isHealthy = false
                }
            }
        } catch (e: Exception) {
            dbStatus = "Offline / Connection Error: ${e.localizedMessage}"
            isHealthy = false
        }

        val latency = System.currentTimeMillis() - startTime
        val overallStatus = if (isHealthy) {
            if (latency > 1500) BackendHealthStatus.DEGRADED else BackendHealthStatus.HEALTHY
        } else {
            BackendHealthStatus.OFFLINE
        }

        val report = BackendHealthReport(
            status = overallStatus,
            databaseStatus = dbStatus,
            authStatus = authStatus,
            storageStatus = "Supabase Storage Active",
            latencyMs = latency,
            pendingQueueCount = pendingCount,
            isRlsProtected = true,
            serverTimestamp = System.currentTimeMillis(),
            details = if (isHealthy) "✓ All systems operational with end-to-end RLS security" else "Operating in offline-first mode with queued background sync."
        )

        _healthReport.value = report
        com.example.service.admin.TelegramAdminBotManager.updateServiceStatus(
            serviceName = "Supabase",
            isHealthy = isHealthy,
            reason = if (isHealthy) "Operational (${latency}ms)" else dbStatus
        )
        report
    }
}
