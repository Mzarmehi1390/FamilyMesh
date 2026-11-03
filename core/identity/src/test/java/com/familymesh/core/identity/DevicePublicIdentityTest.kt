package com.familymesh.core.identity

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DevicePublicIdentityTest {
    @Test
    fun validIdentity_preservesPublicMetadata() {
        val identity = DevicePublicIdentity(
            keyId = "device-key-1",
            publicKeyBytes = byteArrayOf(1, 2, 3),
            createdAtEpochMillis = 100L,
        )

        assertEquals("device-key-1", identity.keyId)
        assertArrayEquals(byteArrayOf(1, 2, 3), identity.publicKeyBytes)
        assertEquals(100L, identity.createdAtEpochMillis)
    }

    @Test
    fun identity_copiesCallerOwnedPublicKeyArray() {
        val supplied = byteArrayOf(1, 2, 3)
        val identity = DevicePublicIdentity("key", supplied, 0L)

        supplied[0] = 9

        assertArrayEquals(byteArrayOf(1, 2, 3), identity.publicKeyBytes)
    }

    @Test
    fun returnedPublicKeyArray_cannotMutateIdentity() {
        val identity = DevicePublicIdentity("key", byteArrayOf(1, 2, 3), 0L)

        val exposed = identity.publicKeyBytes
        exposed[0] = 9

        assertArrayEquals(byteArrayOf(1, 2, 3), identity.publicKeyBytes)
    }

    @Test
    fun equalPublicIdentities_haveEqualContentAndHashCode() {
        val first = DevicePublicIdentity("key", byteArrayOf(1, 2, 3), 10L)
        val second = DevicePublicIdentity("key", byteArrayOf(1, 2, 3), 10L)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun blankKeyId_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DevicePublicIdentity(" ", byteArrayOf(1), 0L)
        }
    }

    @Test
    fun emptyPublicKey_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DevicePublicIdentity("key", byteArrayOf(), 0L)
        }
    }

    @Test
    fun negativeCreationTimestamp_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DevicePublicIdentity("key", byteArrayOf(1), -1L)
        }
    }
}
