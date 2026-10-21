package com.example.messenger.di

import com.example.messenger.data.local.prefs.ThemePreferenceStore
import com.example.messenger.domain.featureflag.FeatureFlags
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.service.ICallService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UiEntryPoints {
    fun userRepository(): IUserRepository
    fun themePreferenceStore(): ThemePreferenceStore
    fun callService(): ICallService
    fun featureFlags(): FeatureFlags
}
