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
    fun rejectsNullLikeOrRandomBytes() {
        assertNull(P256PublicKeyParser.decode(ByteArray(64) { it.toByte() }))
    }

    private fun keyPair(curve: String) = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(curve))
        generateKeyPair()
    }
}
