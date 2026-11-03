package com.familymesh.core.identity

/**
 * Expected failures from a platform-backed device key provider.
 *
 * These failures are intentionally distinct so callers can present safe,
 * accurate recovery guidance without treating every failure as permission
 * to silently replace a device identity.
 */
sealed class DeviceKeyManagerException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class KeyStoreUnavailable(cause: Throwable? = null) :
        DeviceKeyManagerException("Android Keystore is unavailable", cause)

    class KeyInvalidated(cause: Throwable? = null) :
        DeviceKeyManagerException("The device key is no longer usable", cause)

    class KeyMaterialMissing(cause: Throwable? = null) :
        DeviceKeyManagerException("Expected device key material is missing", cause)

    class IdentityAlreadyExists(cause: Throwable? = null) :
        DeviceKeyManagerException("A device identity is already provisioned", cause)

    class UnsupportedKeyConfiguration(cause: Throwable? = null) :
        DeviceKeyManagerException("The reviewed key configuration is unsupported", cause)

    class CorruptKeyMaterial(cause: Throwable? = null) :
        DeviceKeyManagerException("Stored public key metadata is invalid", cause)
}
