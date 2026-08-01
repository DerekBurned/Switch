package com.example.messenger.di

import com.example.messenger.data.service.AndroidSessionCleaner
import com.example.messenger.data.service.FirebasePresenceService
import com.example.messenger.data.service.FirebaseTypingService
import com.example.messenger.domain.service.IPresenceService
import com.example.messenger.domain.service.ISessionCleaner
import com.example.messenger.domain.service.ITypingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {

    @Binds
    @Singleton
    abstract fun bindPresenceService(impl: FirebasePresenceService): IPresenceService

    @Binds
    @Singleton
    abstract fun bindTypingService(impl: FirebaseTypingService): ITypingService

    @Binds
    @Singleton
    abstract fun bindSessionCleaner(impl: AndroidSessionCleaner): ISessionCleaner

}
