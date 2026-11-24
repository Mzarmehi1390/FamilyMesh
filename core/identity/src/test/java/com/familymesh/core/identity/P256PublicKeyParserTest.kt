package com.familymesh.core.identity

import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertEquals
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
    fun rejectsP256PointThatIsNotOnTheCurve() {
        val encoded = keyPair("secp256r1").public.encoded
        val invalidPoint = encoded.withUncompressedPoint(BigInteger.ONE, BigInteger.ONE)

        assertNull(P256PublicKeyParser.decode(invalidPoint))
    }

    @Test
    fun rejectsCoordinateEqualToTheFieldPrime() {
        val encoded = keyPair("secp256r1").public.encoded
        val invalidPoint = encoded.withUncompressedPoint(P256_FIELD_PRIME, BigInteger.ONE)

        assertNull(P256PublicKeyParser.decode(invalidPoint))
    }

    private fun keyPair(curve: String) = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(curve))
        generateKeyPair()
    }

    private fun ByteArray.withUncompressedPoint(x: BigInteger, y: BigInteger): ByteArray {
        val pointOffset = size - UNCOMPRESSED_POINT_SIZE
        assertEquals(UNCOMPRESSED_POINT_PREFIX, this[pointOffset].toInt() and 0xff)
        return copyOf().apply {
            writeCoordinate(x, pointOffset + 1)
            writeCoordinate(y, pointOffset + 1 + COORDINATE_SIZE)
        }
    }

    private fun ByteArray.writeCoordinate(value: BigInteger, offset: Int) {
        require(value.signum() >= 0)
        val encoded = value.toByteArray()
        val unsigned = if (encoded.size == COORDINATE_SIZE + 1 && encoded[0] == 0.toByte()) {
            encoded.copyOfRange(1, encoded.size)
        } else {
            encoded
        }
        require(unsigned.size <= COORDINATE_SIZE)
        fill(0.toByte(), offset, offset + COORDINATE_SIZE)
        System.arraycopy(
            unsigned,
            0,
            this,
            offset + COORDINATE_SIZE - unsigned.size,
            unsigned.size,
        )
    }

    private companion object {
        const val UNCOMPRESSED_POINT_PREFIX = 0x04
        const val UNCOMPRESSED_POINT_SIZE = 65
        const val COORDINATE_SIZE = 32
        val P256_FIELD_PRIME = BigInteger(
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",
            16,
        )
    }
}
