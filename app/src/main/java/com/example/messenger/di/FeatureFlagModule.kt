package com.example.messenger.di

import com.example.messenger.data.remote.firebase.RemoteConfigFeatureFlags
import com.example.messenger.domain.featureflag.FeatureFlags
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagModule {
    @Binds
    @Singleton
    abstract fun bindFeatureFlags(impl: RemoteConfigFeatureFlags): FeatureFlags
}
