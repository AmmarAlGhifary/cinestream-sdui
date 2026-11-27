package com.ammar.network.di

import com.ammar.cinestream.core.network.BuildConfig
import com.ammar.network.session.SessionStore
import com.ammar.network.source.ActionDataSource
import com.ammar.network.source.ActionDataSourceImpl
import com.ammar.network.source.ApiLoggingInterceptor
import com.ammar.network.source.AuthInterceptor
import com.ammar.network.source.SduiApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(sessions: SessionStore): OkHttpClient {
        return OkHttpClient.Builder()
            // Signing first, so the log shows that the header was sent (redacted).
            .addInterceptor(AuthInterceptor(sessions, BuildConfig.BASE_URL.toHttpUrl()))
            .addInterceptor(ApiLoggingInterceptor(enabled = BuildConfig.DEBUG))
            .build()
    }

    @Provides
    @Singleton
    fun provideActionDataSource(okHttpClient: OkHttpClient): ActionDataSource =
        ActionDataSourceImpl(okHttpClient, BuildConfig.BASE_URL.toHttpUrl())

    @Provides
    @Singleton
    fun provideJsonParser(): Json {
        return Json { ignoreUnknownKeys = true }
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideSduiApiService(retrofit: Retrofit): SduiApiService {
        return retrofit.create(SduiApiService::class.java)
    }
}
