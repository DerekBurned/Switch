package com.example.messenger.presentation.featureflag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.featureflag.FeatureFlagState

val LocalFeatureFlags = compositionLocalOf { FeatureFlagState.defaults() }

@Composable
@ReadOnlyComposable
fun featureEnabled(flag: FeatureFlag): Boolean = LocalFeatureFlags.current.isEnabled(flag)
