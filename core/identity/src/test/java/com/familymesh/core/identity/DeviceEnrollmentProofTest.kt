package com.familymesh.core.identity

import com.familymesh.core.common.domain.DeviceEnrollmentRequest
import com.familymesh.core.common.domain.EnrollmentRequestStatus
import com.familymesh.core.common.domain.FamilyRole
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceEnrollmentProofTest {
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }

    private val identity = DevicePublicIdentity(
        keyId = DeviceKeyFingerprint.fromEncodedPublicKey(keyPair.public.encoded),
        publicKeyBytes = keyPair.public.encoded,
        createdAtEpochMillis = 1_000L,
    )

    private val signer = DeviceMessageSigner { message ->
        Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(message)
            sign()
        }
    }

    @Test
    fun proof_roundTripsWithMatchingPublicIdentity() = runBlocking {
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)

        assertTrue(DeviceEnrollmentProofVerifier.verify(proof, identity, nowEpochMillis = 1_500L))
        assertTrue(proof.nonce.size == SignedDeviceEnrollmentRequest.NONCE_LENGTH)
    }

    @Test
    fun separateProofsUseDifferentNonces() = runBlocking {
        val proofSigner = DeviceEnrollmentProofSigner(signer)
        val first = proofSigner.sign(request(), identity)
        val second = proofSigner.sign(request(), identity)

        assertNotEquals(first.nonce.toList(), second.nonce.toList())
    }

    @Test
    fun alteredRequestFieldsInvalidateSignature() = runBlocking {
        val original = DeviceEnrollmentProofSigner(signer).sign(request(), identity)
        val altered = SignedDeviceEnrollmentRequest(
            requestId = original.requestId,
            familySpaceId = "another-family",
            deviceKeyId = original.deviceKeyId,
            requestedRole = original.requestedRole,
            createdAtEpochMillis = original.createdAtEpochMillis,
            expiresAtEpochMillis = original.expiresAtEpochMillis,
            nonce = original.nonce,
            signature = original.signature,
        )

        assertFalse(DeviceEnrollmentProofVerifier.verifySignature(altered, identity))
    }

    @Test
    fun proofIsRejectedForDifferentIdentity() = runBlocking {
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)
        val otherPair = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }
        val otherIdentity = DevicePublicIdentity(
            keyId = DeviceKeyFingerprint.fromEncodedPublicKey(otherPair.public.encoded),
            publicKeyBytes = otherPair.public.encoded,
            createdAtEpochMillis = 1_000L,
        )

        assertFalse(DeviceEnrollmentProofVerifier.verifySignature(proof, otherIdentity))
    }

    @Test
    fun expiredProofIsRejected() = runBlocking {
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)

        assertFalse(DeviceEnrollmentProofVerifier.verify(proof, identity, nowEpochMillis = 2_000L))
    }

    @Test
    fun nonPendingRequestCannotBeSigned() = runBlocking {
        val approved = request().copy(status = EnrollmentRequestStatus.APPROVED)

        try {
            DeviceEnrollmentProofSigner(signer).sign(approved, identity)
            throw AssertionError("Expected non-pending request to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private fun request() = DeviceEnrollmentRequest(
        id = "request-1",
        familySpaceId = "family-1",
        deviceKeyId = identity.keyId,
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 1_000L,
        expiresAtEpochMillis = 2_000L,
    )
}
