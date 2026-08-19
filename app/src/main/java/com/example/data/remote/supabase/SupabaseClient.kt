package com.example.data.remote.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lightweight, direct REST client for Supabase GoTrue Auth, PostgREST, and Storage.
 * Complies with strict security rules:
 * - Uses public/anon key only.
 * - Authenticates requests via Bearer JWT when available.
 * - Fails safely when offline or unconfigured without app crash.
 */
class SupabaseClient(
    private val customUrl: String? = null,
    private val customAnonKey: String? = null
) {
    companion object {
        val instance = SupabaseClient()
    }

    private val TAG = "SupabaseClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val baseUrl: String get() = customUrl ?: SupabaseConfig.supabaseUrl
    val anonKey: String get() = customAnonKey ?: SupabaseConfig.supabaseAnonKey

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun isReady(): Boolean = SupabaseConfig.isConfigured() || (baseUrl.isNotBlank() && anonKey.isNotBlank())

    // --- 1. PostgREST REST API Operations ---

    inner class TableQuery(private val tableName: String) {

        suspend fun select(
            queryParams: Map<String, String> = emptyMap(),
            accessToken: String? = null
        ): SupabaseResult<String> = withContext(Dispatchers.IO) {
            if (!isReady()) {
                return@withContext SupabaseResult.Error("Supabase is not configured yet.")
            }

            try {
                val urlBuilder = "$baseUrl/rest/v1/$tableName".toHttpUrlOrNull()?.newBuilder()
                    ?: return@withContext SupabaseResult.Error("Invalid table URL for $tableName")

                queryParams.forEach { (k, v) ->
                    urlBuilder.addQueryParameter(k, v)
                }

                val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()

                executeRequest(request)
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting from $tableName", e)
                SupabaseResult.Error(e.message ?: "Failed to query $tableName", e)
            }
        }

        suspend fun insert(
            jsonBody: String,
            accessToken: String? = null,
            returnRepresentation: Boolean = true
        ): SupabaseResult<String> = withContext(Dispatchers.IO) {
            if (!isReady()) {
                return@withContext SupabaseResult.Error("Supabase is not configured yet.")
            }

            try {
                val url = "$baseUrl/rest/v1/$tableName"
                val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", if (returnRepresentation) "return=representation" else "return=minimal")
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))

                executeRequest(requestBuilder.build())
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting into $tableName", e)
                SupabaseResult.Error(e.message ?: "Failed to insert into $tableName", e)
            }
        }

        suspend fun upsert(
            jsonBody: String,
            onConflict: String? = null,
            accessToken: String? = null,
            returnRepresentation: Boolean = true
        ): SupabaseResult<String> = withContext(Dispatchers.IO) {
            if (!isReady()) {
                return@withContext SupabaseResult.Error("Supabase is not configured yet.")
            }

            try {
                val urlBuilder = "$baseUrl/rest/v1/$tableName".toHttpUrlOrNull()?.newBuilder()
                    ?: return@withContext SupabaseResult.Error("Invalid table URL for $tableName")

                if (!onConflict.isNullOrBlank()) {
                    urlBuilder.addQueryParameter("on_conflict", onConflict)
                }

                val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"
                val preferValue = buildString {
                    append("resolution=merge-duplicates")
                    if (returnRepresentation) append(",return=representation") else append(",return=minimal")
                }

                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", preferValue)
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                executeRequest(request)
            } catch (e: Exception) {
                Log.e(TAG, "Error upserting into $tableName", e)
                SupabaseResult.Error(e.message ?: "Failed to upsert into $tableName", e)
            }
        }

        suspend fun update(
            queryParams: Map<String, String>,
            jsonBody: String,
            accessToken: String? = null
        ): SupabaseResult<String> = withContext(Dispatchers.IO) {
            if (!isReady()) {
                return@withContext SupabaseResult.Error("Supabase is not configured yet.")
            }

            try {
                val urlBuilder = "$baseUrl/rest/v1/$tableName".toHttpUrlOrNull()?.newBuilder()
                    ?: return@withContext SupabaseResult.Error("Invalid table URL for $tableName")

                queryParams.forEach { (k, v) ->
                    urlBuilder.addQueryParameter(k, v)
                }

                val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .patch(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                executeRequest(request)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating $tableName", e)
                SupabaseResult.Error(e.message ?: "Failed to update $tableName", e)
            }
        }

        suspend fun delete(
            queryParams: Map<String, String>,
            accessToken: String? = null
        ): SupabaseResult<String> = withContext(Dispatchers.IO) {
            if (!isReady()) {
                return@withContext SupabaseResult.Error("Supabase is not configured yet.")
            }

            try {
                val urlBuilder = "$baseUrl/rest/v1/$tableName".toHttpUrlOrNull()?.newBuilder()
                    ?: return@withContext SupabaseResult.Error("Invalid table URL for $tableName")

                queryParams.forEach { (k, v) ->
                    urlBuilder.addQueryParameter(k, v)
                }

                val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", authHeader)
                    .delete()
                    .build()

                executeRequest(request)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting from $tableName", e)
                SupabaseResult.Error(e.message ?: "Failed to delete from $tableName", e)
            }
        }
    }

    fun from(table: String): TableQuery = TableQuery(table)

    // --- 2. Supabase GoTrue Auth API ---

    suspend fun signUp(
        email: String,
        password: String,
        metadata: Map<String, Any?> = emptyMap()
    ): SupabaseResult<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext SupabaseResult.Error("Supabase is not configured yet.")

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
                if (metadata.isNotEmpty()) {
                    put("data", JSONObject(metadata))
                }
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/signup")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = executeRequest(request)
            when (response) {
                is SupabaseResult.Success -> {
                    val auth = parseAuthResponse(response.data)
                    SupabaseResult.Success(auth)
                }
                is SupabaseResult.Error -> SupabaseResult.Error(response.message, response.throwable, response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase signUp failed", e)
            SupabaseResult.Error(e.message ?: "Signup error", e)
        }
    }

    suspend fun signInWithPassword(
        email: String,
        password: String
    ): SupabaseResult<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext SupabaseResult.Error("Supabase is not configured yet.")

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = executeRequest(request)
            when (response) {
                is SupabaseResult.Success -> {
                    val auth = parseAuthResponse(response.data)
                    SupabaseResult.Success(auth)
                }
                is SupabaseResult.Error -> SupabaseResult.Error(response.message, response.throwable, response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase signInWithPassword failed", e)
            SupabaseResult.Error(e.message ?: "SignIn error", e)
        }
    }

    suspend fun refreshToken(
        refreshToken: String
    ): SupabaseResult<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext SupabaseResult.Error("Supabase is not configured yet.")

        try {
            val json = JSONObject().apply {
                put("refresh_token", refreshToken)
            }

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = executeRequest(request)
            when (response) {
                is SupabaseResult.Success -> SupabaseResult.Success(parseAuthResponse(response.data))
                is SupabaseResult.Error -> SupabaseResult.Error(response.message, response.throwable, response.code)
            }
        } catch (e: Exception) {
            SupabaseResult.Error(e.message ?: "Token refresh error", e)
        }
    }

    suspend fun getUser(accessToken: String): SupabaseResult<SupabaseUserDto> = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext SupabaseResult.Error("Supabase is not configured yet.")

        try {
            val request = Request.Builder()
                .url("$baseUrl/auth/v1/user")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = executeRequest(request)
            when (response) {
                is SupabaseResult.Success -> {
                    val obj = JSONObject(response.data)
                    val id = obj.optString("id", "")
                    val email = obj.optString("email", "")
                    SupabaseResult.Success(SupabaseUserDto(id = id, email = email))
                }
                is SupabaseResult.Error -> SupabaseResult.Error(response.message, response.throwable, response.code)
            }
        } catch (e: Exception) {
            SupabaseResult.Error(e.message ?: "Get user error", e)
        }
    }

    // --- 3. Supabase Storage API ---

    suspend fun uploadFile(
        bucket: String,
        path: String,
        mimeType: String,
        fileBytes: ByteArray,
        accessToken: String? = null
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext SupabaseResult.Error("Supabase is not configured yet.")

        try {
            val cleanPath = path.removePrefix("/")
            val url = "$baseUrl/storage/v1/object/$bucket/$cleanPath"
            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

            val mediaType = mimeType.toMediaType()
            val requestBody = fileBytes.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .post(requestBody)
                .build()

            val response = executeRequest(request)
            when (response) {
                is SupabaseResult.Success -> {
                    val publicUrl = getPublicUrl(bucket, cleanPath)
                    SupabaseResult.Success(publicUrl)
                }
                is SupabaseResult.Error -> SupabaseResult.Error(response.message, response.throwable, response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to storage $bucket/$path", e)
            SupabaseResult.Error(e.message ?: "Storage upload error", e)
        }
    }

    fun getPublicUrl(bucket: String, path: String): String {
        val cleanPath = path.removePrefix("/")
        return "$baseUrl/storage/v1/object/public/$bucket/$cleanPath"
    }

    // --- 4. Supabase Edge Functions ---

    suspend fun invokeEdgeFunction(
        functionName: String,
        jsonBody: String,
        accessToken: String? = null
    ): SupabaseResult<String> = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext SupabaseResult.Error("Supabase is not configured yet.")

        try {
            val url = "$baseUrl/functions/v1/$functionName"
            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            executeRequest(request)
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Edge Function $functionName", e)
            SupabaseResult.Error(e.message ?: "Failed to invoke Edge Function $functionName", e)
        }
    }

    // --- Helper Methods ---

    private fun executeRequest(request: Request): SupabaseResult<String> {
        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val code = response.code

            if (response.isSuccessful) {
                SupabaseResult.Success(responseBody)
            } else {
                val errorMsg = try {
                    val json = JSONObject(responseBody)
                    json.optString("message", json.optString("error_description", json.optString("msg", "HTTP $code: $responseBody")))
                } catch (e: Exception) {
                    "HTTP $code: $responseBody"
                }
                SupabaseResult.Error(errorMsg, code = code)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Network error executing Supabase request: ${e.message}")
            SupabaseResult.Error("Network error: ${e.localizedMessage}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error executing Supabase request", e)
            SupabaseResult.Error("Request error: ${e.localizedMessage}", e)
        }
    }

    private fun parseAuthResponse(jsonStr: String): SupabaseAuthResponse {
        val json = JSONObject(jsonStr)
        val accessToken = json.optString("access_token", null)
        val tokenType = json.optString("token_type", null)
        val expiresIn = if (json.has("expires_in")) json.optLong("expires_in") else null
        val refreshToken = json.optString("refresh_token", null)

        var userDto: SupabaseUserDto? = null
        if (json.has("user")) {
            val userObj = json.getJSONObject("user")
            userDto = SupabaseUserDto(
                id = userObj.optString("id", ""),
                email = userObj.optString("email", null),
                phone = userObj.optString("phone", null),
                createdAt = userObj.optString("created_at", null)
            )
        }

        return SupabaseAuthResponse(
            accessToken = accessToken,
            tokenType = tokenType,
            expiresIn = expiresIn,
            refreshToken = refreshToken,
            user = userDto,
            error = json.optString("error", null),
            errorDescription = json.optString("error_description", null),
            msg = json.optString("msg", null)
        )
    }
}
