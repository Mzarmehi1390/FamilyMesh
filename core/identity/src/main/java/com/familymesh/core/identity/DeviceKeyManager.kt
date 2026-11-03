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
     * Returns the device's existing public identity or creates one using the
     * reviewed platform and protocol configuration.
     *
     * This contract alone does not establish family membership or trust.
     */
    suspend fun getOrCreatePublicIdentity(): DevicePublicIdentity
}
