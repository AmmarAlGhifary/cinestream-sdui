package com.ammar.network.di

import com.ammar.network.source.NetworkDataSource
import com.ammar.network.source.NetworkDataSourceImpl
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
    abstract fun bindSduiDataSource(
        retrofitDataSourceImpl: NetworkDataSourceImpl
    ): NetworkDataSource
}