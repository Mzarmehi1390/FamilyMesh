package com.familymesh.core.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Test

class DevicePublicIdentityTest {
    @Test
    fun validIdentity_preservesPublicMetadata() {
        val identity = DevicePublicIdentity(
            keyId = "device-key-1",
            publicKeyBytes = listOf(1, 2, 3),
            createdAtEpochMillis = 100L,
        )

        assertEquals("device-key-1", identity.keyId)
        assertEquals(listOf<Byte>(1, 2, 3), identity.publicKeyBytes)
        assertEquals(100L, identity.createdAtEpochMillis)
    }

    @Test
    fun identity_copiesCallerOwnedPublicKeyList() {
        val supplied = mutableListOf<Byte>(1, 2, 3)
        val identity = DevicePublicIdentity("key", supplied, 0L)

        supplied[0] = 9

        assertEquals(listOf<Byte>(1, 2, 3), identity.publicKeyBytes)
        assertNotSame(supplied, identity.publicKeyBytes)
    }

    @Test
    fun blankKeyId_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DevicePublicIdentity(" ", listOf(1), 0L)
        }
    }

    @Test
    fun emptyPublicKey_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DevicePublicIdentity("key", emptyList(), 0L)
        }
    }

    @Test
    fun negativeCreationTimestamp_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DevicePublicIdentity("key", listOf(1), -1L)
        }
    }
}
