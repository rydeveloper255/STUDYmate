package com.example.data.remote.telegram

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for the Telegram Bot API.
 */
interface TelegramApiService {
    /**
     * Basic Telegram Bot API connection & credential health check.
     * Returns basic information about the bot in form of a TelegramUser object.
     *
     * Official documentation: https://core.telegram.org/bots/api#getme
     */
    @GET("bot{token}/getMe")
    suspend fun getMe(
        @Path("token") token: String
    ): Response<TelegramResponse<TelegramUser>>

    /**
     * Send message to a target channel or chat.
     *
     * Official documentation: https://core.telegram.org/bots/api#sendmessage
     */
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body request: SendMessageRequest
    ): Response<TelegramResponse<TelegramMessage>>
}

