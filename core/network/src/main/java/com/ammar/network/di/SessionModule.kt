package com.ammar.network.di

import android.content.Context
import com.ammar.cinestream.core.network.BuildConfig
import com.ammar.network.session.AesGcmCipher
import com.ammar.network.session.AndroidKeystoreKey
import com.ammar.network.session.EncryptedStorage
import com.ammar.network.session.SessionRepository
import com.ammar.network.session.SessionRepositoryImpl
import com.ammar.network.session.SessionStore
import com.ammar.network.session.SharedPreferencesStore
import com.ammar.network.source.createAuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    /** Excluded from backup and device transfer in the app's backup rules. Keep the names in step. */
    private const val PREFERENCES = "cinestream_session"
    private const val KEY_ALIAS = "cinestream_session"

    @Provides
    @Singleton
    fun provideSessionStore(@ApplicationContext context: Context): SessionStore {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val cipher = AesGcmCipher(AndroidKeystoreKey(KEY_ALIAS))
        return SessionStore(EncryptedStorage(SharedPreferencesStore(preferences), cipher), Clock.systemUTC())
    }

    @Provides
    @Singleton
    fun provideSessionRepository(client: OkHttpClient, store: SessionStore): SessionRepository =
        SessionRepositoryImpl(
            createAuthApi(client, BuildConfig.BASE_URL.toHttpUrl()),
            store,
            Clock.systemUTC()
        )
}
