package com.example.data.remote.telegram

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    /**
     * Send photo attachment to a target channel or chat.
     *
     * Official documentation: https://core.telegram.org/bots/api#sendphoto
     */
    @Multipart
    @POST("bot{token}/sendPhoto")
    suspend fun sendPhoto(
        @Path("token") token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part("caption") caption: RequestBody? = null,
        @Part("parse_mode") parseMode: RequestBody? = null,
        @Part photo: MultipartBody.Part
    ): Response<TelegramResponse<TelegramMessage>>

    /**
     * Send document / video attachment to a target channel or chat.
     *
     * Official documentation: https://core.telegram.org/bots/api#senddocument
     */
    @Multipart
    @POST("bot{token}/sendDocument")
    suspend fun sendDocument(
        @Path("token") token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part("caption") caption: RequestBody? = null,
        @Part("parse_mode") parseMode: RequestBody? = null,
        @Part document: MultipartBody.Part
    ): Response<TelegramResponse<TelegramMessage>>

    /**
     * Edit text of a message previously sent via Telegram Bot API.
     *
     * Official documentation: https://core.telegram.org/bots/api#editmessagetext
     */
    @POST("bot{token}/editMessageText")
    suspend fun editMessageText(
        @Path("token") token: String,
        @Body request: EditMessageTextRequest
    ): Response<TelegramResponse<TelegramMessage>>

    /**
     * Answer incoming callback query from inline buttons.
     *
     * Official documentation: https://core.telegram.org/bots/api#answercallbackquery
     */
    @POST("bot{token}/answerCallbackQuery")
    suspend fun answerCallbackQuery(
        @Path("token") token: String,
        @Body request: AnswerCallbackQueryRequest
    ): Response<TelegramResponse<Boolean>>

    /**
     * Fetch incoming updates from Telegram for authorized admin polling.
     *
     * Official documentation: https://core.telegram.org/bots/api#getupdates
     */
    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @retrofit2.http.Query("offset") offset: Long? = null,
        @retrofit2.http.Query("limit") limit: Int? = 20,
        @retrofit2.http.Query("timeout") timeout: Int? = 0
    ): Response<TelegramResponse<List<TelegramUpdate>>>
}

