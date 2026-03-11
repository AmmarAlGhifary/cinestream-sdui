package com.ammar.network.di

import com.ammar.network.source.FirebaseDataSource
import com.ammar.network.source.FirebaseDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindFirebaseDataSource(
        impl: FirebaseDataSourceImpl
    ): FirebaseDataSource
}