package com.familymesh.core.identity

/**
 * Public, protocol-neutral description of a device key.
 *
 * The byte sequence is opaque until a reviewed protocol defines its encoding.
 * Private key material is deliberately absent from this model.
 */
class DevicePublicIdentity(
    val keyId: String,
    publicKeyBytes: ByteArray,
    val createdAtEpochMillis: Long,
) {
    private val encodedPublicKey: ByteArray = publicKeyBytes.copyOf()

    /** Returns a copy so callers cannot mutate this model's internal bytes. */
    val publicKeyBytes: ByteArray
        get() = encodedPublicKey.copyOf()

    init {
        require(keyId.isNotBlank()) { "Key ID must not be blank" }
        require(keyId.length <= MAX_KEY_ID_LENGTH) { "Key ID is too long" }
        require(encodedPublicKey.isNotEmpty()) { "Public key must not be empty" }
        require(createdAtEpochMillis >= 0L) { "Creation timestamp must not be negative" }
    }

    override fun equals(other: Any?): Boolean =
        other is DevicePublicIdentity &&
            keyId == other.keyId &&
            encodedPublicKey.contentEquals(other.encodedPublicKey) &&
            createdAtEpochMillis == other.createdAtEpochMillis

    override fun hashCode(): Int {
        var result = keyId.hashCode()
        result = 31 * result + encodedPublicKey.contentHashCode()
        result = 31 * result + createdAtEpochMillis.hashCode()
        return result
    }

    companion object {
        const val MAX_KEY_ID_LENGTH = 128
    }
}
