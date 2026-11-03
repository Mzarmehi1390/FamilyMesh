package com.familymesh.core.identity

import java.util.Base64

/**
 * Decodes the single canonical unpadded Base64URL representation emitted by
 * FamilyMesh transfer envelopes. Rejects padding and alternate encodings.
 */
internal object CanonicalBase64Url {
    private val alphabet = Regex("[A-Za-z0-9_-]+")

    fun decode(value: String): ByteArray {
        require(value.isNotEmpty() && alphabet.matches(value)) {
            "Value is not unpadded Base64URL."
        }
        val decoded = Base64.getUrlDecoder().decode(value)
        val canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded)
        require(canonical == value) { "Value is not canonically encoded Base64URL." }
        return decoded
    }
}
