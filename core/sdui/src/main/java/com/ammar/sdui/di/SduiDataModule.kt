package com.ammar.sdui.di

import com.ammar.sdui.data.repository.SduiRepositoryImpl
import com.ammar.sdui.domain.repository.SduiRepository
import com.ammar.sdui.presentation.registry.CineStreamBlueprint
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ammaralghifary.blueprint.Blueprint
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SduiDataModule {

    @Binds
    @Singleton
    abstract fun bindSduiRepository(
        impl: SduiRepositoryImpl
    ): SduiRepository

    companion object {
        @Provides
        @Singleton
        fun provideBlueprint(): Blueprint = CineStreamBlueprint.instance
    }
}
