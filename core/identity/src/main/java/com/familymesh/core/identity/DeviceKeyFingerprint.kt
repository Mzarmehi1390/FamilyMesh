package com.familymesh.core.identity

import java.security.MessageDigest
import java.util.Locale

/**
 * Versioned, domain-separated local fingerprint for an encoded public key.
 *
 * This fingerprints the X.509 SubjectPublicKeyInfo bytes returned by JCA.
 * It is not the final wire-level COSE_Key identifier.
 */
object DeviceKeyFingerprint {
    private val domain = "FamilyMesh:device-key:v1\u0000".toByteArray(Charsets.UTF_8)

    fun fromEncodedPublicKey(encodedPublicKey: ByteArray): String {
        require(encodedPublicKey.isNotEmpty()) { "Public key must not be empty" }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(domain)
        val fingerprint = digest.digest(encodedPublicKey)
        return fingerprint.joinToString(separator = "") { byte ->
            String.format(Locale.ROOT, "%02x", byte.toInt() and 0xff)
        }
    }
}
