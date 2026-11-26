package com.familymesh.core.identity

import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun rejectsTrailingBytesAfterSubjectPublicKeyInfo() {
        val encoded = keyPair("secp256r1").public.encoded
        assertNull(P256PublicKeyParser.decode(encoded + byteArrayOf(0x00)))
    }

    @Test
    fun rejectsNonMinimalDerSequenceLength() {
        val encoded = keyPair("secp256r1").public.encoded
        val contentLength = encoded[1].toInt() and 0xff
        assertTrue("P-256 SPKI fixture should use a short-form DER length", contentLength < 128)
        val nonCanonical = byteArrayOf(encoded[0], 0x81.toByte(), contentLength.toByte()) +
            encoded.copyOfRange(2, encoded.size)

        assertNull(P256PublicKeyParser.decode(nonCanonical))
    }

    @Test
    fun rejectsP256PointAtInfinity() {
        val encoded = p256SubjectPublicKeyInfo(byteArrayOf(0x00))
        assertNull(P256PublicKeyParser.decode(encoded))
    }

    private fun p256SubjectPublicKeyInfo(point: ByteArray): ByteArray {
        val algorithmIdentifier = hexBytes("301306072a8648ce3d020106082a8648ce3d030107")
        val bitString = der(0x03, byteArrayOf(0x00) + point)
        return der(0x30, algorithmIdentifier + bitString)
    }

    private fun hexBytes(value: String): ByteArray =
        ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }

    private fun der(tag: Int, content: ByteArray): ByteArray =
        byteArrayOf(tag.toByte()) + derLength(content.size) + content

    private fun derLength(length: Int): ByteArray {
        if (length < 128) return byteArrayOf(length.toByte())
        val byteCount = (32 - Integer.numberOfLeadingZeros(length) + 7) / 8
        val encoded = ByteArray(byteCount) { index ->
            (length ushr ((byteCount - index - 1) * 8)).toByte()
        }
        return byteArrayOf((0x80 or byteCount).toByte()) + encoded
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
