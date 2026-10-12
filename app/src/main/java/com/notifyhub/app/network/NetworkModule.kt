package com.notifyhub.app.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a fresh ApiService bound to the given base URL + bearer token.
 * Rebuilt by PollingService whenever the user's connection settings change,
 * since Retrofit's base URL is fixed at construction time.
 */
object NetworkModule {

    fun buildApiService(baseUrl: String, apiToken: String): ApiService {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val authInterceptor = okhttp3.Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (apiToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiToken")
            }
            chain.proceed(requestBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
