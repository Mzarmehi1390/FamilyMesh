package com.familymesh.core.identity

/**
 * Public, protocol-neutral description of a device key.
 *
 * The byte sequence is opaque until a reviewed protocol defines its encoding.
 * Private key material is deliberately absent from this model.
 */
class DevicePublicIdentity(
    val keyId: String,
    publicKeyBytes: List<Byte>,
    val createdAtEpochMillis: Long,
) {
    val publicKeyBytes: List<Byte> = publicKeyBytes.toList()

    init {
        require(keyId.isNotBlank()) { "Key ID must not be blank" }
        require(keyId.length <= MAX_KEY_ID_LENGTH) { "Key ID is too long" }
        require(this.publicKeyBytes.isNotEmpty()) { "Public key must not be empty" }
        require(createdAtEpochMillis >= 0L) { "Creation timestamp must not be negative" }
    }

    override fun equals(other: Any?): Boolean =
        other is DevicePublicIdentity &&
            keyId == other.keyId &&
            publicKeyBytes == other.publicKeyBytes &&
            createdAtEpochMillis == other.createdAtEpochMillis

    override fun hashCode(): Int {
        var result = keyId.hashCode()
        result = 31 * result + publicKeyBytes.hashCode()
        result = 31 * result + createdAtEpochMillis.hashCode()
        return result
    }

    companion object {
        const val MAX_KEY_ID_LENGTH = 128
    }
}
