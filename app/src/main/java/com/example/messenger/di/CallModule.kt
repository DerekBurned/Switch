package com.example.messenger.di

import com.example.messenger.data.remote.call.AgoraCallService
import com.example.messenger.data.remote.call.FirebaseCallSignalingService
import com.example.messenger.domain.service.ICallService
import com.example.messenger.domain.service.ICallSignalingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CallModule {

    @Binds
    @Singleton
    abstract fun bindCallService(impl: AgoraCallService): ICallService

    @Binds
    @Singleton
    abstract fun bindCallSignalingService(impl: FirebaseCallSignalingService): ICallSignalingService
}
