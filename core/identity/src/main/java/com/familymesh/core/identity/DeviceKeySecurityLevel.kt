package com.familymesh.core.identity

/** Observed Android Keystore protection level; not a remote attestation. */
enum class DeviceKeySecurityLevel {
    SOFTWARE,
    TRUSTED_ENVIRONMENT,
    STRONGBOX,
    UNKNOWN,
}

/** Optional capability for reporting the protection level of the local key. */
interface DeviceKeySecurityInfoProvider {
    suspend fun getSecurityLevel(): DeviceKeySecurityLevel
}

/** Signs application-defined messages without exposing private key bytes. */
interface DeviceMessageSigner {
    suspend fun sign(message: ByteArray): ByteArray
}
