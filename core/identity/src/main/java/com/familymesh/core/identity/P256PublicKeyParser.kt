package com.familymesh.core.identity

import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECFieldFp
import java.security.spec.X509EncodedKeySpec

/**
 * Parses only P-256 EC SubjectPublicKeyInfo keys.
 *
 * The current protocol profile is P-256 ECDSA/SHA-256. Accepting arbitrary EC
 * curves at verification boundaries would silently broaden that profile.
 */
internal object P256PublicKeyParser {
    fun decode(encoded: ByteArray): ECPublicKey? {
        return try {
            val key = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(encoded)) as? ECPublicKey
                ?: return null
            // KeyFactory may accept BER-like aliases (for example, a
            // non-minimal DER length) and silently normalize them on output.
            // Fingerprints are over the original SPKI bytes, so require one
            // byte-stable DER representation for each accepted public key.
            val canonicalEncoding = key.encoded ?: return null
            if (!canonicalEncoding.contentEquals(encoded)) return null
            if (isValidP256(key)) key else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Validates an already-decoded provider key without reparsing its SPKI.
     * The Android Keystore certificate is platform-produced, so callers that
     * already hold its ECPublicKey can enforce the same curve/point checks
     * without an additional KeyFactory allocation.
     */
    fun isValidP256(key: ECPublicKey): Boolean {
        return try {
            val expected = AlgorithmParameters.getInstance("EC").run {
                init(ECGenParameterSpec("secp256r1"))
                getParameterSpec(ECParameterSpec::class.java)
            }
            key.params.isP256EquivalentTo(expected) && key.hasValidP256Point()
        } catch (_: Exception) {
            false
        }
    }

    private fun ECParameterSpec.isP256EquivalentTo(expected: ECParameterSpec): Boolean {
        val actualField = curve.field as? ECFieldFp ?: return false
        val expectedField = expected.curve.field as? ECFieldFp ?: return false
        return actualField.p == expectedField.p &&
            curve.a == expected.curve.a &&
            curve.b == expected.curve.b &&
            generator == expected.generator &&
            order == expected.order &&
            cofactor == expected.cofactor
    }
}


/**
 * JCA providers differ in how eagerly they validate imported EC points.
 * Enforce the P-256 field bounds and curve equation at this shared trust
 * boundary so every enrollment-artifact verifier gets the same guarantee.
 */
private fun ECPublicKey.hasValidP256Point(): Boolean {
    val field = params.curve.field as? ECFieldFp ?: return false
    val prime = field.p
    val point = w
    if (point == ECPoint.POINT_INFINITY) return false

    val x = point.affineX ?: return false
    val y = point.affineY ?: return false
    if (x.signum() < 0 || x >= prime || y.signum() < 0 || y >= prime) return false

    val curve = params.curve
    val left = y.multiply(y).mod(prime)
    val right = x.multiply(x).multiply(x)
        .add(curve.a.multiply(x))
        .add(curve.b)
        .mod(prime)
    return left == right
}
