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
     * Loads the currently provisioned public identity.
     *
     * Returns null only when the provider can establish that no identity has
     * ever been provisioned in its current installation state. If metadata
     * and the platform key disagree, implementations must report a typed
     * failure rather than treating the device as new.
     */
    suspend fun getPublicIdentity(): DevicePublicIdentity?

    /**
     * Explicitly provisions a new device identity after the caller has
     * completed the reviewed user-facing setup/trust flow.
     *
     * Implementations must not overwrite an existing identity. A missing key
     * after reinstall, restore, invalidation, or metadata loss must not be
     * interpreted as authorization to silently create a replacement.
     *
     * This operation alone does not establish family membership or trust.
     */
    suspend fun createPublicIdentity(): DevicePublicIdentity
}
