package com.familymesh.core.identity

import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class P256PublicKeyParserTest {
    @Test
    fun acceptsP256PublicKey() {
        val pair = keyPair("secp256r1")
        assertNotNull(P256PublicKeyParser.decode(pair.public.encoded))
    }

    @Test
    fun rejectsP384PublicKey() {
        val pair = keyPair("secp384r1")
        assertNull(P256PublicKeyParser.decode(pair.public.encoded))
    }

    @Test
    fun rejectsP521PublicKey() {
        val pair = keyPair("secp521r1")
        assertNull(P256PublicKeyParser.decode(pair.public.encoded))
    }

    @Test
    fun rejectsEmptyBytes() {
        assertNull(P256PublicKeyParser.decode(byteArrayOf()))
    }

    @Test
    fun rejectsMalformedShortBytes() {
        assertNull(P256PublicKeyParser.decode(byteArrayOf(0x30, 0x00)))
    }

    @Test
    fun rejectsTruncatedP256Spki() {
        val pair = keyPair("secp256r1")
        val encoded = pair.public.encoded
        if (encoded.size > 10) {
            assertNull(P256PublicKeyParser.decode(encoded.copyOf(encoded.size / 2)))
        }
    }

    @Test
    fun rejectsNullLikeOrRandomBytes() {
        assertNull(P256PublicKeyParser.decode(ByteArray(64) { it.toByte() }))
    }

    @Test
    fun rejectsNonEcAlgorithmIdentifier() {
        // Minimal invalid DER that is not an EC key
        assertNull(P256PublicKeyParser.decode(byteArrayOf(0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00)))
    }

    private fun keyPair(curve: String) = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(curve))
        generateKeyPair()
    }
}
