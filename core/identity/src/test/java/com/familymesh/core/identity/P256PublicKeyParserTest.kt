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
    fun rejectsEmptyEncoding() {
        assertNull(P256PublicKeyParser.decode(ByteArray(0)))
    }

    @Test
    fun rejectsTruncatedEncoding() {
        val pair = keyPair("secp256r1")
        val truncated = pair.public.encoded.copyOf(8)
        assertNull(P256PublicKeyParser.decode(truncated))
    }

    @Test
    fun rejectsNonEcEncoding() {
        // A minimal invalid SPKI that is not an EC key.
        val invalid = byteArrayOf(0x30, 0x00)
        assertNull(P256PublicKeyParser.decode(invalid))
    }

    @Test
    fun rejectsMalformedDer() {
        val malformed = byteArrayOf(0x30, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00)
        assertNull(P256PublicKeyParser.decode(malformed))
    }

    private fun keyPair(curve: String) = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(curve))
        generateKeyPair()
    }
}
