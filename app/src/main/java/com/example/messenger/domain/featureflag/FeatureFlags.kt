package com.example.messenger.domain.featureflag

import kotlinx.coroutines.flow.StateFlow

interface FeatureFlags {
    val state: StateFlow<FeatureFlagState>

    fun isEnabled(flag: FeatureFlag): Boolean = state.value.isEnabled(flag)

    suspend fun awaitFresh()

    fun start()
}
