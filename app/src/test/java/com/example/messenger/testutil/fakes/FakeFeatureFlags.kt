package com.example.messenger.testutil.fakes

import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.featureflag.FeatureFlagState
import com.example.messenger.domain.featureflag.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFeatureFlags(off: Set<FeatureFlag> = emptySet()) : FeatureFlags {

    override val state = MutableStateFlow(FeatureFlagState.defaults(off))

    var awaitFreshCalls = 0

    fun setOff(vararg flags: FeatureFlag) {
        state.value = FeatureFlagState.defaults(flags.toSet())
    }

    override suspend fun awaitFresh() {
        awaitFreshCalls++
    }

    override fun start() = Unit
}
