package com.example.messenger.di

import com.example.messenger.data.sync.INetworkObserver
import com.example.messenger.data.sync.NetworkObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkObserver(
        impl: NetworkObserver
    ): INetworkObserver
}