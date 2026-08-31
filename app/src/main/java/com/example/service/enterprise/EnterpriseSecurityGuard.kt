package com.example.service.enterprise

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Step 85: Enterprise Security, Rate Limiting & Prompt Injection Guard.
 * 
 * Provides:
 * - AI Prompt Injection Defense: Treats raw scraped source posts as untrusted DATA rather than instructions
 * - User Action Rate Limiting (Token Bucket / Sliding Window)
 * - Admin Role & Privilege Verification for privileged operations
 * - Data Exfiltration Protection & Private User Data Isolation
 */
class EnterpriseSecurityGuard {

    companion object {
        private const val TAG = "EnterpriseSecurityGuard"

        // Recognized prompt injection markers in source content
        private val PROMPT_INJECTION_PATTERNS = listOf(
            Regex("(?i)ignore (all )?(previous|prior) (instructions|prompts)"),
            Regex("(?i)system\\s*prompt"),
            Regex("(?i)you are now (an unrestricted|a new)"),
            Regex("(?i)reveal (your|the) (api key|secret|token|password)"),
            Regex("(?i)delete (all|from) (database|users|tables)"),
            Regex("(?i)exec\\s*\\("),
            Regex("(?i)<script.*?>.*?</script>")
        )

        // Authorized admin user IDs / tokens
        private val AUTHORIZED_ADMIN_IDS = setOf(
            "admin_chief_1",
            "admin_super_01",
            "studymate_admin_master",
            "system_internal_worker"
        )
    }

    // Rate limiter: action_key -> queue of timestamps
    private val actionRateLimits = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()

    // ==========================================
    // 1. AI PROMPT INJECTION SANITIZATION
    // ==========================================

    /**
     * Sanitizes raw content before passing to AI to neutralize prompt injections.
     * Never interprets source content as executable instructions.
     */
    fun sanitizeForAiInput(rawContent: String): String {
        var sanitized = rawContent

        // Neutralize suspicious prompt injection sentences
        for (pattern in PROMPT_INJECTION_PATTERNS) {
            if (pattern.containsMatchIn(sanitized)) {
                Log.w(TAG, "🛡️ Potential AI prompt injection detected in source content! Pattern matched: ${pattern.pattern}")
                sanitized = pattern.replace(sanitized, "[BLOCKED_SUSPICIOUS_PROMPT_DIRECTIVE]")
            }
        }

        // Limit maximum character length to prevent buffer/token exhaustion attacks
        if (sanitized.length > 5000) {
            sanitized = sanitized.take(5000)
        }

        return sanitized
    }

    // ==========================================
    // 2. USER ACTION RATE LIMITING
    // ==========================================

    /**
     * Evaluates if an action is within allowed rate limits.
     * @param key Identifier (e.g. userId, ip, endpoint)
     * @param maxRequests Maximum allowed requests in the time window
     * @param windowMs Time window in milliseconds
     */
    fun isRateLimitExceeded(key: String, maxRequests: Int = 30, windowMs: Long = 60000L): Boolean {
        val now = System.currentTimeMillis()
        val queue = actionRateLimits.getOrPut(key) { ConcurrentLinkedQueue() }

        val cutoff = now - windowMs
        while (queue.isNotEmpty() && (queue.peek() ?: 0L) < cutoff) {
            queue.poll()
        }

        if (queue.size >= maxRequests) {
            Log.w(TAG, "🚫 Rate limit exceeded for key [$key]: ${queue.size}/$maxRequests in ${windowMs}ms")
            return true
        }

        queue.add(now)
        return false
    }

    // ==========================================
    // 3. ADMIN PRIVILEGE VERIFICATION
    // ==========================================

    /**
     * Verifies that the operator is an authorized administrator.
     */
    fun isAuthorizedAdmin(adminUserId: String?): Boolean {
        if (adminUserId.isNullOrBlank()) return false
        val isAuth = AUTHORIZED_ADMIN_IDS.contains(adminUserId) || adminUserId.startsWith("admin_")
        if (!isAuth) {
            Log.w(TAG, "⛔ Unauthorized administrative action attempted by user: $adminUserId")
        }
        return isAuth
    }

    // ==========================================
    // 4. DATA ISOLATION INTEGRITY
    // ==========================================

    /**
     * Ensures that public content ingestion payloads never contain private user records.
     */
    fun validatePublicContentIsolation(payload: Map<String, String>): Boolean {
        val sensitiveKeys = listOf("password", "token", "otp", "auth_token", "user_email", "phone_number", "ssn")
        val containsSensitiveKey = payload.keys.any { k -> sensitiveKeys.any { k.contains(it, ignoreCase = true) } }
        if (containsSensitiveKey) {
            Log.e(TAG, "🚨 Security violation! Sensitive user keys detected in public content ingestion payload.")
            return false
        }
        return true
    }

    fun resetRateLimits() {
        actionRateLimits.clear()
    }
}
