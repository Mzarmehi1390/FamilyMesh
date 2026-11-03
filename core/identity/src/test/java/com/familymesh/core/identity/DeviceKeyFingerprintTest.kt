package com.familymesh.core.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DeviceKeyFingerprintTest {
    @Test
    fun fingerprint_isStableForSamePublicKeyBytes() {
        val first = DeviceKeyFingerprint.fromEncodedPublicKey(byteArrayOf(1, 2, 3))
        val second = DeviceKeyFingerprint.fromEncodedPublicKey(byteArrayOf(1, 2, 3))

        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun fingerprint_changesWhenPublicKeyBytesChange() {
        val first = DeviceKeyFingerprint.fromEncodedPublicKey(byteArrayOf(1, 2, 3))
        val second = DeviceKeyFingerprint.fromEncodedPublicKey(byteArrayOf(1, 2, 4))

        assertNotEquals(first, second)
    }

    @Test
    fun emptyPublicKey_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceKeyFingerprint.fromEncodedPublicKey(byteArrayOf())
        }
    }
}
