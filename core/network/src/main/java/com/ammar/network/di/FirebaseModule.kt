package com.ammar.network.di

import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance("https://cinestream-sdui-backend-default-rtdb.asia-southeast1.firebasedatabase.app")
    }

    @Provides
    @Singleton
    fun provideJson() : Json {
        return Json {
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        }
    }
}