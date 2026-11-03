package com.familymesh.core.identity

import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceKeyManagerExceptionTest {
    @Test
    fun keyStoreUnavailable_isTypedFailure() {
        assertTrue(DeviceKeyManagerException.KeyStoreUnavailable() is DeviceKeyManagerException)
    }

    @Test
    fun invalidatedKey_isDistinctFailure() {
        assertTrue(DeviceKeyManagerException.KeyInvalidated() is DeviceKeyManagerException)
    }

    @Test
    fun missingKeyMaterial_isDistinctFailure() {
        assertTrue(DeviceKeyManagerException.KeyMaterialMissing() is DeviceKeyManagerException)
    }

    @Test
    fun existingIdentity_isNotSilentlyOverwritten() {
        assertTrue(DeviceKeyManagerException.IdentityAlreadyExists() is DeviceKeyManagerException)
    }

    @Test
    fun unsupportedConfiguration_isDistinctFailure() {
        assertTrue(DeviceKeyManagerException.UnsupportedKeyConfiguration() is DeviceKeyManagerException)
    }

    @Test
    fun corruptMetadata_isDistinctFailure() {
        assertTrue(DeviceKeyManagerException.CorruptKeyMaterial() is DeviceKeyManagerException)
    }
}
