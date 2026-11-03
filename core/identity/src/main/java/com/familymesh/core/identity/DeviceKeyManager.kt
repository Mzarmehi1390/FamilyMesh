package com.familymesh.core.identity

/**
 * Boundary for a future platform-backed device key provider.
 *
 * Implementations must keep private key material inside the platform's
 * protected key boundary and must never return or persist it through this API.
 * Algorithm selection, key generation, signing, rotation, and deletion remain
 * intentionally undefined until the identity protocol is reviewed.
 */
interface DeviceKeyManager {
    /**
     * Returns the existing public identity, or creates it once using the
     * reviewed platform and protocol configuration.
     *
     * Repeated successful calls for the same key lifecycle must return the
     * same identity. An invalidated or missing key must not be silently
     * replaced: callers need an explicit, separately designed recovery or
     * re-enrollment flow before a new identity can be trusted.
     *
     * This contract alone does not establish family membership or trust.
     *
     * @throws DeviceKeyManagerException.KeyStoreUnavailable when secure
     * key storage cannot be accessed.
     * @throws DeviceKeyManagerException.KeyInvalidated when the existing key
     * can no longer be used.
     * @throws DeviceKeyManagerException.UnsupportedKeyConfiguration when the
     * reviewed configuration is unavailable on this device.
     * @throws DeviceKeyManagerException.CorruptKeyMaterial when stored public
     * metadata cannot be validated.
     */
    suspend fun getOrCreatePublicIdentity(): DevicePublicIdentity
}
