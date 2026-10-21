package com.example.messenger.domain.featureflag

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class FeatureFlagStateTest {

    @Test
    fun `every flag is on by default`() {
        val state = FeatureFlagState.defaults()

        FeatureFlag.entries.forEach { assertThat(state.isEnabled(it)).isTrue() }
        assertThat(state.disabledKeys()).isEmpty()
    }

    @Test
    fun `turning a parent off turns its children off`() {
        val state = FeatureFlagState.defaults(setOf(FeatureFlag.CALLS))

        assertThat(state.isEnabled(FeatureFlag.CALLS)).isFalse()
        assertThat(state.isEnabled(FeatureFlag.CALLS_VIDEO)).isFalse()
        assertThat(state.isEnabled(FeatureFlag.CALLS_PIP)).isFalse()
        assertThat(state.isEnabled(FeatureFlag.VOICE_MESSAGES)).isTrue()
    }

    @Test
    fun `a grandparent being off turns the grandchild off`() {
        val state = FeatureFlagState.defaults(setOf(FeatureFlag.CLIP_PLAYBACK))

        assertThat(state.isEnabled(FeatureFlag.CLIP_PLAYBACK_SPEED)).isFalse()
    }

    @Test
    fun `a child can be off while its parent stays on`() {
        val state = FeatureFlagState.defaults(setOf(FeatureFlag.CALLS_VIDEO))

        assertThat(state.isEnabled(FeatureFlag.CALLS)).isTrue()
        assertThat(state.isEnabled(FeatureFlag.CALLS_VIDEO)).isFalse()
        assertThat(state.isEnabled(FeatureFlag.CALLS_SPEAKER)).isTrue()
    }

    @Test
    fun `remote values override the build default in both directions`() {
        val state = FeatureFlagState.resolve(setOf(FeatureFlag.ACCOUNTS_SWITCH)) { key ->
            when (key) {
                "accounts_switch" -> true
                "search" -> false
                else -> null
            }
        }

        assertThat(state.isEnabled(FeatureFlag.ACCOUNTS_SWITCH)).isTrue()
        assertThat(state.isEnabled(FeatureFlag.SEARCH)).isFalse()
        assertThat(state.isEnabled(FeatureFlag.SEARCH_RECENT)).isFalse()
        assertThat(state.disabledKeys()).containsExactly("search")
    }

    @Test
    fun `off list parsing trims blanks and skips unknown keys`() {
        val csv = " calls , ,settings_language,not_a_flag"

        assertThat(FeatureFlag.parseKeys(csv))
            .containsExactlyInAnyOrder(FeatureFlag.CALLS, FeatureFlag.SETTINGS_LANGUAGE)
        assertThat(FeatureFlag.unknownKeys(csv)).containsExactly("not_a_flag")
    }

    @Test
    fun `empty off list parses to nothing`() {
        assertThat(FeatureFlag.parseKeys("")).isEmpty()
        assertThat(FeatureFlag.unknownKeys("")).isEmpty()
    }

    @Test
    fun `keys are unique and valid remote config parameter names`() {
        val keys = FeatureFlag.entries.map { it.key }

        assertThat(keys.toSet().size).isEqualTo(keys.size)
        keys.forEach { assertThat(Regex("^[a-z][a-z0-9_]*$").matches(it)).isTrue() }
    }

    @Test
    fun `fromKey finds every flag by its key`() {
        FeatureFlag.entries.forEach { assertThat(FeatureFlag.fromKey(it.key)).isEqualTo(it) }
    }

    @Test
    fun `the switch hierarchy matches the catalog`() {
        val expectedParents = mapOf(
            FeatureFlag.CALLS_VIDEO to FeatureFlag.CALLS,
            FeatureFlag.CALLS_FLIP_CAMERA to FeatureFlag.CALLS,
            FeatureFlag.CALLS_SPEAKER to FeatureFlag.CALLS,
            FeatureFlag.CALLS_PIP to FeatureFlag.CALLS,
            FeatureFlag.CALLS_FLOATING_BUBBLE to FeatureFlag.CALLS,
            FeatureFlag.CLIP_SEEK to FeatureFlag.CLIP_PLAYBACK,
            FeatureFlag.CLIP_MINI_PLAYER to FeatureFlag.CLIP_PLAYBACK,
            FeatureFlag.CLIP_PLAYBACK_SPEED to FeatureFlag.CLIP_MINI_PLAYER,
            FeatureFlag.MEDIA_CAMERA to FeatureFlag.MEDIA_SENDING,
            FeatureFlag.MEDIA_PICKER_FILTERS to FeatureFlag.MEDIA_SENDING,
            FeatureFlag.MESSAGE_COPY to FeatureFlag.MESSAGE_MENU,
            FeatureFlag.MESSAGE_DELETE_FOR_ME to FeatureFlag.MESSAGE_MENU,
            FeatureFlag.MESSAGE_DELETE_FOR_EVERYONE to FeatureFlag.MESSAGE_MENU,
            FeatureFlag.MESSAGE_SWIPE_REPLY to FeatureFlag.MESSAGE_REPLY,
            FeatureFlag.MESSAGE_REPLY_JUMP to FeatureFlag.MESSAGE_REPLY,
            FeatureFlag.CHAT_LIST_MARK_READ to FeatureFlag.CHAT_LIST_SWIPE_ACTIONS,
            FeatureFlag.CHAT_LIST_DELETE to FeatureFlag.CHAT_LIST_SWIPE_ACTIONS,
            FeatureFlag.CHAT_LIST_DELETE_FOR_EVERYONE to FeatureFlag.CHAT_LIST_SWIPE_ACTIONS,
            FeatureFlag.CHAT_LIST_EDIT to FeatureFlag.CHAT_LIST_SWIPE_ACTIONS,
            FeatureFlag.SEARCH_RECENT to FeatureFlag.SEARCH,
            FeatureFlag.PROFILE_AVATAR_CAMERA to FeatureFlag.PROFILE_AVATAR_UPLOAD,
            FeatureFlag.APPEARANCE_CUSTOM_WALLPAPER to FeatureFlag.APPEARANCE_WALLPAPERS,
            FeatureFlag.APPEARANCE_WALLPAPER_INTENSITY to FeatureFlag.APPEARANCE_WALLPAPERS,
            FeatureFlag.NOTIFICATION_BUBBLES to FeatureFlag.NOTIFICATION_SHORTCUTS,
        )

        FeatureFlag.entries.forEach { flag ->
            assertThat(flag.parent).isEqualTo(expectedParents[flag])
        }
    }

    @Test
    fun `a parent being off turns every one of its descendants off`() {
        val clipOff = FeatureFlagState.defaults(setOf(FeatureFlag.CLIP_MINI_PLAYER))

        assertThat(clipOff.isEnabled(FeatureFlag.CLIP_PLAYBACK_SPEED)).isFalse()
        assertThat(clipOff.isEnabled(FeatureFlag.CLIP_SEEK)).isTrue()
        assertThat(clipOff.isEnabled(FeatureFlag.CLIP_PLAYBACK)).isTrue()
    }
}
