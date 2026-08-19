package com.example.data.remote.supabase

import android.util.Log
import com.example.BuildConfig

/**
 * Supabase configuration provider that safely reads credentials from BuildConfig.
 * Adheres strictly to security rules:
 * - Uses public anon key only.
 * - Never hardcodes secrets.
 * - Handles missing/placeholder values gracefully without crashing.
 */
object SupabaseConfig {
    private const val TAG = "SupabaseConfig"

    private fun getBuildConfigField(fieldName: String): String {
        return try {
            val field = BuildConfig::class.java.getField(fieldName)
            (field.get(null) as? String)?.trim() ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    val supabaseUrl: String
        get() {
            return try {
                val url = getBuildConfigField("SUPABASE_URL")
                if (url.isNotBlank() && url != "https://your-project.supabase.co" && !url.contains("dummy")) {
                    url.removeSuffix("/")
                } else {
                    ""
                }
            } catch (e: Throwable) {
                Log.w(TAG, "SUPABASE_URL not found in BuildConfig", e)
                ""
            }
        }

    val supabaseAnonKey: String
        get() {
            return try {
                val key = getBuildConfigField("SUPABASE_ANON_KEY")
                if (key.isNotBlank() && key != "eyJhbGciOi..." && !key.contains("dummy")) {
                    key
                } else {
                    ""
                }
            } catch (e: Throwable) {
                Log.w(TAG, "SUPABASE_ANON_KEY not found in BuildConfig", e)
                ""
            }
        }

    /**
     * Checks if valid Supabase configuration is present.
     */
    fun isConfigured(): Boolean {
        val url = supabaseUrl
        val key = supabaseAnonKey
        return url.isNotBlank() && url.startsWith("https://") && key.isNotBlank()
    }
}
