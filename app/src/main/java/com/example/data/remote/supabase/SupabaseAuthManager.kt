package com.example.data.remote.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manages Supabase Auth credentials, active tokens, and current user identity.
 * Persists session safely to SharedPreferences and provides authenticated user_id.
 */
class SupabaseAuthManager(
    private val context: Context,
    private val client: SupabaseClient
) {
    private val TAG = "SupabaseAuthManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("studymate_supabase_auth", Context.MODE_PRIVATE)

    private val KEY_ACCESS_TOKEN = "sb_access_token"
    private val KEY_REFRESH_TOKEN = "sb_refresh_token"
    private val KEY_USER_ID = "sb_user_id"
    private val KEY_USER_EMAIL = "sb_user_email"
    private val KEY_EXPIRES_AT = "sb_expires_at"
    private val KEY_ANON_STUDENT_ID = "sb_anon_student_id"

    private val _currentUserId = MutableStateFlow(getStoredUserId())
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(isSessionValid())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun getStoredUserId(): String {
        val saved = prefs.getString(KEY_USER_ID, null)
        if (!saved.isNullOrBlank()) return saved

        // If no authenticated user yet, provide a persistent device scholar UUID
        var anonId = prefs.getString(KEY_ANON_STUDENT_ID, null)
        if (anonId.isNullOrBlank()) {
            anonId = "scholar_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            prefs.edit().putString(KEY_ANON_STUDENT_ID, anonId).apply()
        }
        return anonId
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (token.isNullOrBlank()) return null

        // If expired, refresh token in background if possible
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            Log.d(TAG, "Access token expired, will attempt refresh")
        }
        return token
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun isSessionValid(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        return !token.isNullOrBlank() && !userId.isNullOrBlank()
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String,
        expiresInSeconds: Long? = 3600L
    ) {
        val expiresAt = System.currentTimeMillis() + ((expiresInSeconds ?: 3600L) * 1000L)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken ?: "")
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()

        _currentUserId.value = userId
        _isAuthenticated.value = true
        Log.d(TAG, "Supabase session saved for user: $userId ($email)")
    }

    fun associateFirebaseOrLocalUser(userId: String, email: String) {
        if (prefs.getString(KEY_USER_ID, null).isNullOrBlank()) {
            prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .apply()
            _currentUserId.value = userId
        }
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_EXPIRES_AT)
            .apply()

        val fallbackId = getStoredUserId()
        _currentUserId.value = fallbackId
        _isAuthenticated.value = false
        Log.d(TAG, "Supabase session cleared")
    }

    suspend fun refreshSessionIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = getRefreshToken() ?: return@withContext false
        if (refreshToken.isBlank()) return@withContext false

        when (val result = client.refreshToken(refreshToken)) {
            is SupabaseResult.Success -> {
                val data = result.data
                if (!data.accessToken.isNullOrBlank() && data.user != null) {
                    saveSession(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken ?: refreshToken,
                        userId = data.user.id,
                        email = data.user.email ?: getUserEmail(),
                        expiresInSeconds = data.expiresIn ?: 3600L
                    )
                    true
                } else false
            }
            is SupabaseResult.Error -> {
                Log.w(TAG, "Failed to refresh Supabase session: ${result.message}")
                false
            }
        }
    }
}
