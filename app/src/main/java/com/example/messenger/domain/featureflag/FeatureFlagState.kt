package com.example.messenger.domain.featureflag

data class FeatureFlagState(private val values: Map<FeatureFlag, Boolean>) {

    fun isEnabled(flag: FeatureFlag): Boolean {
        var current: FeatureFlag? = flag
        while (current != null) {
            if (values[current] == false) return false
            current = current.parent
        }
        return true
    }

    fun disabledKeys(): List<String> =
        FeatureFlag.entries.filter { values[it] == false }.map { it.key }

    companion object {
        fun defaults(offByDefault: Set<FeatureFlag> = emptySet()): FeatureFlagState =
            resolve(offByDefault) { null }

        fun resolve(
            offByDefault: Set<FeatureFlag>,
            remoteValue: (String) -> Boolean?,
        ): FeatureFlagState = FeatureFlagState(
            FeatureFlag.entries.associateWith { flag ->
                remoteValue(flag.key) ?: (flag !in offByDefault)
            },
        )
    }
}
