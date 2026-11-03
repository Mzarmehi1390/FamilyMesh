package com.familymesh.core.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdentityLifecyclePolicyTest {
    @Test
    fun bothArtifactsAbsent_isNotProvisioned() {
        assertState(
            StoredIdentityMetadataState.ABSENT,
            PlatformDeviceKeyState.ABSENT,
            matches = false,
            expected = DeviceIdentityLifecycleState.NOT_PROVISIONED,
        )
    }

    @Test
    fun matchingMetadataAndKey_isReady() {
        assertState(
            StoredIdentityMetadataState.PRESENT,
            PlatformDeviceKeyState.PRESENT,
            matches = true,
            expected = DeviceIdentityLifecycleState.READY,
        )
    }

    @Test
    fun mismatchedMetadataAndKey_requiresRecovery() {
        assertState(
            StoredIdentityMetadataState.PRESENT,
            PlatformDeviceKeyState.PRESENT,
            matches = false,
            expected = DeviceIdentityLifecycleState.RECOVERY_REQUIRED,
        )
    }

    @Test
    fun metadataWithoutPlatformKey_requiresRecovery() {
        assertState(
            StoredIdentityMetadataState.PRESENT,
            PlatformDeviceKeyState.ABSENT,
            matches = false,
            expected = DeviceIdentityLifecycleState.RECOVERY_REQUIRED,
        )
    }

    @Test
    fun platformKeyWithoutMetadata_requiresRecovery() {
        assertState(
            StoredIdentityMetadataState.ABSENT,
            PlatformDeviceKeyState.PRESENT,
            matches = false,
            expected = DeviceIdentityLifecycleState.RECOVERY_REQUIRED,
        )
    }

    @Test
    fun invalidatedKey_requiresRecovery() {
        assertState(
            StoredIdentityMetadataState.PRESENT,
            PlatformDeviceKeyState.INVALIDATED,
            matches = false,
            expected = DeviceIdentityLifecycleState.RECOVERY_REQUIRED,
        )
    }

    @Test
    fun corruptMetadata_requiresRecovery() {
        assertState(
            StoredIdentityMetadataState.CORRUPT,
            PlatformDeviceKeyState.PRESENT,
            matches = false,
            expected = DeviceIdentityLifecycleState.RECOVERY_REQUIRED,
        )
    }

    @Test
    fun unavailableKeystore_isTemporarilyUnavailable() {
        assertState(
            StoredIdentityMetadataState.PRESENT,
            PlatformDeviceKeyState.UNAVAILABLE,
            matches = false,
            expected = DeviceIdentityLifecycleState.TEMPORARILY_UNAVAILABLE,
        )
    }

    private fun assertState(
        metadata: StoredIdentityMetadataState,
        key: PlatformDeviceKeyState,
        matches: Boolean,
        expected: DeviceIdentityLifecycleState,
    ) {
        assertEquals(expected, DeviceIdentityLifecyclePolicy.evaluate(metadata, key, matches))
    }
}
