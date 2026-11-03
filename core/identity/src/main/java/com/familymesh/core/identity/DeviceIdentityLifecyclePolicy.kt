package com.familymesh.core.identity

/**
 * Presence of the public identity metadata stored by the app.
 */
enum class StoredIdentityMetadataState {
    ABSENT,
    PRESENT,
    CORRUPT,
}

/**
 * Observed state of the platform-protected private key.
 */
enum class PlatformDeviceKeyState {
    ABSENT,
    PRESENT,
    INVALIDATED,
    UNAVAILABLE,
}

/**
 * Safe, protocol-neutral outcome of inspecting local identity artifacts.
 */
enum class DeviceIdentityLifecycleState {
    NOT_PROVISIONED,
    READY,
    RECOVERY_REQUIRED,
    TEMPORARILY_UNAVAILABLE,
}

/**
 * Pure policy for reconciling app metadata with platform key state.
 *
 * This policy never creates, deletes, or repairs key material. In particular,
 * a mismatch is a recovery condition, not permission to generate a new key.
 */
object DeviceIdentityLifecyclePolicy {
    fun evaluate(
        metadataState: StoredIdentityMetadataState,
        keyState: PlatformDeviceKeyState,
        metadataMatchesKey: Boolean,
    ): DeviceIdentityLifecycleState {
        if (keyState == PlatformDeviceKeyState.UNAVAILABLE) {
            return DeviceIdentityLifecycleState.TEMPORARILY_UNAVAILABLE
        }

        if (
            metadataState == StoredIdentityMetadataState.CORRUPT ||
            keyState == PlatformDeviceKeyState.INVALIDATED
        ) {
            return DeviceIdentityLifecycleState.RECOVERY_REQUIRED
        }

        return when {
            metadataState == StoredIdentityMetadataState.ABSENT &&
                keyState == PlatformDeviceKeyState.ABSENT ->
                DeviceIdentityLifecycleState.NOT_PROVISIONED

            metadataState == StoredIdentityMetadataState.PRESENT &&
                keyState == PlatformDeviceKeyState.PRESENT &&
                metadataMatchesKey ->
                DeviceIdentityLifecycleState.READY

            else -> DeviceIdentityLifecycleState.RECOVERY_REQUIRED
        }
    }
}
