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
    fun decode(encoded: ByteArray): ECPublicKey? = try {
        val key = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(encoded)) as? ECPublicKey
            ?: return null
        val expected = AlgorithmParameters.getInstance("EC").run {
            init(ECGenParameterSpec("secp256r1"))
            getParameterSpec(ECParameterSpec::class.java)
        }
        if (key.params.isP256EquivalentTo(expected)) key else null
    } catch (_: Exception) {
        null
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
