package com.example.messenger.data.remote.firebase

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.example.messenger.BuildConfig
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.testutil.AndroidLogExtension
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigInfo
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class RemoteConfigFeatureFlagsTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val values = mutableMapOf<String, FirebaseRemoteConfigValue>()
    private val configUpdates = slot<ConfigUpdateListener>()
    private val remoteConfig = mockk<FirebaseRemoteConfig> {
        every { ensureInitialized() } returns completed(mockk<FirebaseRemoteConfigInfo>(relaxed = true))
        every { fetch(any()) } returns completed<Void?>(null)
        every { activate() } returns completed(true)
        every { fetchAndActivate() } returns completed(true)
        every { setConfigSettingsAsync(any()) } returns completed<Void?>(null)
        every { addOnConfigUpdateListener(capture(configUpdates)) } returns mockk(relaxed = true)
        every { getValue(any()) } answers {
            values[firstArg()] ?: value(FirebaseRemoteConfig.VALUE_SOURCE_STATIC, false)
        }
    }
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val flags = RemoteConfigFeatureFlags(remoteConfig, crashlytics)

    @Test
    fun `a value fetched from the console turns a flag off`() = runTest {
        values[FeatureFlag.VOICE_MESSAGES.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, false)

        flags.awaitFresh()

        assertThat(flags.isEnabled(FeatureFlag.VOICE_MESSAGES)).isFalse()
        assertThat(flags.isEnabled(FeatureFlag.CALLS)).isTrue()
    }

    @Test
    fun `a parameter missing from the console keeps every flag at its build default`() = runTest {
        val offInThisBuild = FeatureFlag.parseKeys(BuildConfig.FEATURES_OFF_BY_DEFAULT)

        flags.awaitFresh()

        FeatureFlag.entries.forEach { assertThat(flags.isEnabled(it)).isEqualTo(it !in offInThisBuild) }
    }

    @Test
    fun `start publishes the values saved on the device as soon as they load`() {
        every { remoteConfig.fetchAndActivate() } returns pending()
        values[FeatureFlag.VOICE_MESSAGES.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, false)

        flags.start()

        assertThat(flags.isEnabled(FeatureFlag.VOICE_MESSAGES)).isFalse()
    }

    @Test
    fun `start publishes the values fetched at launch`() {
        every { remoteConfig.ensureInitialized() } returns pending()
        values[FeatureFlag.VOICE_MESSAGES.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, false)

        flags.start()

        assertThat(flags.isEnabled(FeatureFlag.VOICE_MESSAGES)).isFalse()
    }

    @Test
    fun `a realtime update from the console is activated and published`() {
        flags.start()
        values[FeatureFlag.VIDEO_NOTES.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, false)

        configUpdates.captured.onUpdate(mockk(relaxed = true))

        verify { remoteConfig.activate() }
        assertThat(flags.isEnabled(FeatureFlag.VIDEO_NOTES)).isFalse()
    }

    @Test
    fun `starting twice registers the console listeners once`() {
        flags.start()
        flags.start()

        verify(exactly = 1) { remoteConfig.fetchAndActivate() }
        verify(exactly = 1) { remoteConfig.addOnConfigUpdateListener(any()) }
    }

    @Test
    fun `an in-app default value is ignored`() = runTest {
        values[FeatureFlag.CALLS.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT, false)

        flags.awaitFresh()

        assertThat(flags.isEnabled(FeatureFlag.CALLS)).isTrue()
    }

    @Test
    fun `a console value that is not a boolean keeps the build default`() = runTest {
        values[FeatureFlag.CALLS.key] = mockk {
            every { source } returns FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
            every { asBoolean() } throws IllegalArgumentException("not a boolean")
        }

        flags.awaitFresh()

        assertThat(flags.isEnabled(FeatureFlag.CALLS)).isTrue()
    }

    @Test
    fun `a failed fetch still publishes the activated console values`() = runTest {
        every { remoteConfig.fetch(any()) } returns failed(IllegalStateException("offline"))
        values[FeatureFlag.VIDEO_NOTES.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, false)

        flags.awaitFresh()

        assertThat(flags.isEnabled(FeatureFlag.VIDEO_NOTES)).isFalse()
    }

    @Test
    fun `flags turned off are reported to Crashlytics`() = runTest {
        values[FeatureFlag.VOICE_MESSAGES.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, false)
        values[FeatureFlag.SEARCH.key] = value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, false)

        flags.awaitFresh()

        verify { crashlytics.setCustomKey("flags_off", "voice_messages,search") }
    }

    @Test
    fun `a refresh waits for saved values, fetches anything older than five minutes and activates it`() = runTest {
        flags.awaitFresh()

        verifyOrder {
            remoteConfig.ensureInitialized()
            remoteConfig.fetch(300L)
            remoteConfig.activate()
        }
    }

    @Test
    fun `every key turned off by default in release is a known flag`() {
        assertThat(FeatureFlag.parseKeys(BuildConfig.FEATURES_OFF_IN_RELEASE)).isNotEmpty()
        assertThat(FeatureFlag.unknownKeys(BuildConfig.FEATURES_OFF_IN_RELEASE)).isEmpty()
    }

    private fun value(source: Int, asBoolean: Boolean): FirebaseRemoteConfigValue = mockk {
        every { this@mockk.source } returns source
        every { asBoolean() } returns asBoolean
    }

    private fun <T> completed(value: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.exception } returns null
        every { task.result } returns value
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            firstArg<OnCompleteListener<T>>().onComplete(task)
            task
        }
        return task
    }

    private fun <T> pending(): Task<T> {
        val task = mockk<Task<T>>()
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } returns task
        return task
    }

    private fun <T> failed(error: Exception): Task<T> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns error
    }
}
