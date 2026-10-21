package com.example.messenger.data.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class KeystoreKeyWrapperTest {

    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    @Test
    fun aWrappedKeyUnwrapsToTheSameBytes() {
        val wrapper = KeystoreKeyWrapper()

        assertArrayEquals(key, wrapper.unwrap(wrapper.wrap(key)))
    }

    @Test
    fun aNewWrapperInstanceUnwrapsWhatAnotherWrapped() {
        val wrapped = KeystoreKeyWrapper().wrap(key)

        assertArrayEquals(key, KeystoreKeyWrapper().unwrap(wrapped))
    }

    @Test
    fun twoWrapsOfTheSameKeyDiffer() {
        val wrapper = KeystoreKeyWrapper()

        assertFalse(wrapper.wrap(key).contentEquals(wrapper.wrap(key)))
    }

    @Test(expected = AEADBadTagException::class)
    fun aTamperedWrappedKeyIsRejected() {
        val wrapper = KeystoreKeyWrapper()
        val wrapped = wrapper.wrap(key)
        wrapped[12] = (wrapped[12].toInt() xor 1).toByte()

        wrapper.unwrap(wrapped)
    }
}
