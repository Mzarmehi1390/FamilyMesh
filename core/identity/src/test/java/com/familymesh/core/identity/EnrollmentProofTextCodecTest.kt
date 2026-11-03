package com.familymesh.core.identity

import com.familymesh.core.common.domain.DeviceEnrollmentRequest
import com.familymesh.core.common.domain.FamilyRole
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrollmentProofTextCodecTest {
    private val pair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
    private val identity = DevicePublicIdentity(
        keyId = DeviceKeyFingerprint.fromEncodedPublicKey(pair.public.encoded),
        publicKeyBytes = pair.public.encoded,
        createdAtEpochMillis = 500L,
    )
    private val signer = DeviceMessageSigner { message ->
        Signature.getInstance("SHA256withECDSA").run {
            initSign(pair.private)
            update(message)
            sign()
        }
    }

    @Test
    fun encodedProofCanBeDecodedAndVerified() = runBlocking {
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)
        val decoded = EnrollmentProofTextCodec.decode(EnrollmentProofTextCodec.encode(proof, identity))

        assertEquals(proof.requestId, decoded.proof.requestId)
        assertEquals(proof.familySpaceId, decoded.proof.familySpaceId)
        assertEquals(proof.nonce.toList(), decoded.proof.nonce.toList())
        assertEquals(identity, decoded.identity)
        assertTrue(DeviceEnrollmentProofVerifier.verify(decoded.proof, decoded.identity, 1_500L))
    }

    @Test
    fun malformedPrefixIsRejected() {
        try {
            EnrollmentProofTextCodec.decode("NOT-FMER1")
            throw AssertionError("Expected malformed prefix to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun trailingBytesAreRejected() = runBlocking {
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)
        val encoded = EnrollmentProofTextCodec.encode(proof, identity)
        val raw = java.util.Base64.getUrlDecoder().decode(encoded.removePrefix("FMER1."))
        val altered = "FMER1." + java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw + byteArrayOf(0))
        try {
            EnrollmentProofTextCodec.decode(altered)
            throw AssertionError("Expected trailing bytes to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun oversizedInputIsRejected() {
        try {
            EnrollmentProofTextCodec.decode("FMER1." + "A".repeat(7000))
            throw AssertionError("Expected oversized input to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun changingTransferredSignatureInvalidatesVerification() = runBlocking {
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)
        val decoded = EnrollmentProofTextCodec.decode(EnrollmentProofTextCodec.encode(proof, identity))
        val tampered = SignedDeviceEnrollmentRequest(
            requestId = decoded.proof.requestId,
            familySpaceId = decoded.proof.familySpaceId,
            deviceKeyId = decoded.proof.deviceKeyId,
            requestedRole = decoded.proof.requestedRole,
            createdAtEpochMillis = decoded.proof.createdAtEpochMillis,
            expiresAtEpochMillis = decoded.proof.expiresAtEpochMillis,
            nonce = decoded.proof.nonce,
            signature = decoded.proof.signature.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() },
        )

        assertFalse(DeviceEnrollmentProofVerifier.verifySignature(tampered, identity))
    }

    private fun request() = DeviceEnrollmentRequest(
        id = "request-codec-1",
        familySpaceId = "family-codec-1",
        deviceKeyId = identity.keyId,
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 1_000L,
        expiresAtEpochMillis = 2_000L,
    )
}
