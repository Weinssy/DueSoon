package com.duesoon.app.data.remote

import android.content.Context
import com.duesoon.app.core.crypto.SecureStorage
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object NetworkModule {

    private const val BASE_URL = "https://api.duesoon.com/v1/"

    fun provideSecureStorage(context: Context): SecureStorage {
        return SecureStorage(context)
    }

    fun provideOkHttpClient(secureStorage: SecureStorage): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            
            // Inject bearer token if available
            val token = secureStorage.getAuthToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    fun provideSyncApiService(okHttpClient: OkHttpClient): SyncApiService {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(SyncApiService::class.java)
    }
}
