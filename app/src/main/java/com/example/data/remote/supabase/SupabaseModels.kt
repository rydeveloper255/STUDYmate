package com.example.data.remote.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for Supabase GoTrue Auth, PostgREST queries, and Storage.
 */

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseUserDto? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
    @Json(name = "msg") val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStorageUploadResponse(
    @Json(name = "Key") val key: String? = null,
    @Json(name = "Id") val id: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "statusCode") val statusCode: String? = null
)

sealed class SupabaseResult<out T> {
    data class Success<out T>(val data: T) : SupabaseResult<T>()
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val code: Int? = null,
        val errorId: String? = null
    ) : SupabaseResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
}

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val userEmail: String,
    val expiresAtMillis: Long
)
