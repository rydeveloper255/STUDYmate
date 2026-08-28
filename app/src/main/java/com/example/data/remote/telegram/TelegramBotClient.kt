package com.example.data.remote.telegram

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * HTTP Client and Retrofit initializer for Telegram Bot API communication.
 *
 * Security:
 * - Logging is strictly disabled or redacted to prevent the token from being captured in logs.
 */
object TelegramBotClient {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            // Level.NONE ensures bot token embedded in URL paths is never written to logcat
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val apiService: TelegramApiService by lazy {
        Retrofit.Builder()
            .baseUrl(TelegramBotConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelegramApiService::class.java)
    }
}
