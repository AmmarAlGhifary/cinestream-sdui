package com.ammar.sdui.di

import com.ammar.sdui.domain.repository.SduiRepository
import com.ammar.sdui.data.repository.SduiRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SduiDataModule {

    @Binds
    @Singleton
    abstract fun bindSduiRepository(
        impl: SduiRepositoryImpl
    ): SduiRepository
}