package com.familymesh.core.identity

import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import com.familymesh.core.common.domain.TrustedInitialOwner
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrollmentDecisionTest {
    private val pair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
    private val ownerKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(pair.public.encoded)
    private val owner = TrustedInitialOwner(
        familySpaceId = "family-1",
        ownerMemberId = "owner-1",
        ownerDeviceKeyId = ownerKeyId,
        ownerPublicKeyBase64 = Base64.getEncoder().encodeToString(pair.public.encoded),
        establishedAtEpochMillis = 100L,
        pinnedAtEpochMillis = 150L,
    )
    private val request = PendingEnrollmentRequest(
        requestId = "request-1",
        familySpaceId = "family-1",
        deviceKeyId = "applicant-key",
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 200L,
        expiresAtEpochMillis = 1_000L,
        receivedAtEpochMillis = 250L,
    )

    @Test
    fun signedDecisionRoundTripsAndVerifiesAgainstPinnedOwner() {
        val decision = signedDecision()
        val decoded = EnrollmentDecisionTextCodec.decode(EnrollmentDecisionTextCodec.encode(decision))

        assertEquals(EnrollmentDecisionOutcome.APPROVED, decoded.outcome)
        assertEquals(request.requestId, decoded.requestId)
        assertTrue(EnrollmentDecisionVerifier.verify(decoded, request, owner, 500L))
    }

    @Test
    fun decisionForDifferentRequestIsRejected() {
        val decision = signedDecision()
        val other = request.copy(requestId = "other-request")

        assertFalse(EnrollmentDecisionVerifier.verify(decision, other, owner, 500L))
    }

    @Test
    fun decisionFromDifferentFamilyIsRejected() {
        val decision = signedDecision()
        val otherOwner = owner.copy(familySpaceId = "other-family")

        assertFalse(EnrollmentDecisionVerifier.verify(decision, request, otherOwner, 500L))
    }

    @Test
    fun futureOrOutOfWindowDecisionIsRejected() {
        val decision = signedDecision(at = 1_001L)

        assertFalse(EnrollmentDecisionVerifier.verify(decision, request, owner, 1_100L))
        assertFalse(EnrollmentDecisionVerifier.verify(decision, request, owner, 900L))
    }

    @Test
    fun alteredOutcomeInvalidatesSignature() {
        val signed = signedDecision()
        val altered = SignedEnrollmentDecision(
            signed.familySpaceId, signed.requestId, signed.applicantDeviceKeyId,
            signed.ownerDeviceKeyId, EnrollmentDecisionOutcome.REJECTED,
            signed.decidedAtEpochMillis, signed.signature,
        )

        assertFalse(EnrollmentDecisionVerifier.verify(altered, request, owner, 500L))
    }

    @Test
    fun alteredOwnerKeyIsRejected() {
        val otherPair = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }
        val otherOwner = owner.copy(
            ownerDeviceKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(otherPair.public.encoded),
            ownerPublicKeyBase64 = Base64.getEncoder().encodeToString(otherPair.public.encoded),
        )

        assertFalse(EnrollmentDecisionVerifier.verify(signedDecision(), request, otherOwner, 500L))
    }

    @Test
    fun codecRejectsTrailingBytes() {
        val encoded = EnrollmentDecisionTextCodec.encode(signedDecision())
        val raw = Base64.getUrlDecoder().decode(encoded.removePrefix("FMOD1."))
        val altered = "FMOD1." + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw + byteArrayOf(0))

        try {
            EnrollmentDecisionTextCodec.decode(altered)
            throw AssertionError("Expected trailing bytes to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private fun signedDecision(at: Long = 300L): SignedEnrollmentDecision {
        val unsigned = SignedEnrollmentDecision(
            request.familySpaceId, request.requestId, request.deviceKeyId,
            ownerKeyId, EnrollmentDecisionOutcome.APPROVED, at, byteArrayOf(1),
        )
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(pair.private)
            update(unsigned.signingBytes())
            sign()
        }
        return SignedEnrollmentDecision(
            unsigned.familySpaceId, unsigned.requestId, unsigned.applicantDeviceKeyId,
            unsigned.ownerDeviceKeyId, unsigned.outcome, unsigned.decidedAtEpochMillis, signature,
        )
    }
}
