package com.familymesh.core.identity

import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import com.familymesh.core.common.domain.TrustedInitialOwner
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrollmentAcceptanceTest {
    private val ownerPair = keyPair()
    private val applicantPair = keyPair()
    private val ownerKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(ownerPair.public.encoded)
    private val applicantKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(applicantPair.public.encoded)
    private val owner = TrustedInitialOwner(
        familySpaceId = "family-1",
        ownerMemberId = "owner-1",
        ownerDeviceKeyId = ownerKeyId,
        ownerPublicKeyBase64 = Base64.getEncoder().encodeToString(ownerPair.public.encoded),
        establishedAtEpochMillis = 100L,
        pinnedAtEpochMillis = 150L,
    )
    private val request = PendingEnrollmentRequest(
        requestId = "request-1",
        familySpaceId = "family-1",
        deviceKeyId = applicantKeyId,
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 200L,
        expiresAtEpochMillis = 2_000L,
        receivedAtEpochMillis = 250L,
    )
    private val applicantIdentity = DevicePublicIdentity(applicantKeyId, applicantPair.public.encoded, 200L)

    @Test
    fun acceptanceRoundTripsAndVerifiesBothSignatures() = runBlocking {
        val acceptance = signedAcceptance()
        val decoded = EnrollmentAcceptanceTextCodec.decode(EnrollmentAcceptanceTextCodec.encode(acceptance))

        assertEquals(request.requestId, decoded.decision.requestId)
        assertTrue(EnrollmentAcceptanceVerifier.verify(decoded, request, owner, 500L))
    }

    @Test
    fun acceptanceForDifferentRequestIsRejected() = runBlocking {
        val other = request.copy(requestId = "other")
        assertFalse(EnrollmentAcceptanceVerifier.verify(signedAcceptance(), other, owner, 500L))
    }

    @Test
    fun acceptanceWithDifferentApplicantKeyIsRejected() = runBlocking {
        val otherPair = keyPair()
        val otherId = DeviceKeyFingerprint.fromEncodedPublicKey(otherPair.public.encoded)
        val unsigned = SignedEnrollmentAcceptance(
            decision(), otherId, otherPair.public.encoded, 400L, byteArrayOf(1),
        )
        val forged = SignedEnrollmentAcceptance(
            unsigned.decision, unsigned.applicantDeviceKeyId, unsigned.applicantPublicKeyBytes,
            unsigned.acceptedAtEpochMillis, sign(applicantPair.private, unsigned.signingBytes()),
        )
        assertFalse(EnrollmentAcceptanceVerifier.verify(forged, request, owner, 500L))
    }

    @Test
    fun futureAndExpiredAcceptancesAreRejected() = runBlocking {
        assertFalse(EnrollmentAcceptanceVerifier.verify(signedAcceptance(at = 2_000L), request, owner, 2_100L))
        assertFalse(EnrollmentAcceptanceVerifier.verify(signedAcceptance(), request, owner, 350L))
    }

    @Test
    fun rejectedDecisionCannotBeAccepted() = runBlocking {
        val rejected = signDecision(EnrollmentDecisionOutcome.REJECTED)
        try {
            EnrollmentAcceptanceSigner(DeviceMessageSigner { sign(applicantPair.private, it) })
                .sign(rejected, applicantIdentity, 400L)
            throw AssertionError("Expected rejected decision to be refused")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun alteredAcceptanceSignatureIsRejected() = runBlocking {
        val signed = signedAcceptance()
        val altered = SignedEnrollmentAcceptance(
            signed.decision, signed.applicantDeviceKeyId, signed.applicantPublicKeyBytes,
            signed.acceptedAtEpochMillis, signed.signature.reversedArray(),
        )
        assertFalse(EnrollmentAcceptanceVerifier.verify(altered, request, owner, 500L))
    }

    @Test
    fun codecRejectsTrailingBytes() = runBlocking {
        val encoded = EnrollmentAcceptanceTextCodec.encode(signedAcceptance())
        val raw = Base64.getUrlDecoder().decode(encoded.removePrefix("FMJA1."))
        val altered = "FMJA1." + Base64.getUrlEncoder().withoutPadding().encodeToString(raw + byteArrayOf(0))
        try {
            EnrollmentAcceptanceTextCodec.decode(altered)
            throw AssertionError("Expected trailing bytes to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private suspend fun signedAcceptance(at: Long = 400L): SignedEnrollmentAcceptance =
        EnrollmentAcceptanceSigner(DeviceMessageSigner { sign(applicantPair.private, it) })
            .sign(decision(), applicantIdentity, at)

    private fun decision(outcome: EnrollmentDecisionOutcome = EnrollmentDecisionOutcome.APPROVED) =
        signDecision(outcome)

    private fun signDecision(outcome: EnrollmentDecisionOutcome): SignedEnrollmentDecision {
        val unsigned = SignedEnrollmentDecision(
            request.familySpaceId, request.requestId, request.deviceKeyId, ownerKeyId,
            outcome, 300L, byteArrayOf(1),
        )
        return SignedEnrollmentDecision(
            unsigned.familySpaceId, unsigned.requestId, unsigned.applicantDeviceKeyId,
            unsigned.ownerDeviceKeyId, unsigned.outcome, unsigned.decidedAtEpochMillis,
            sign(ownerPair.private, unsigned.signingBytes()),
        )
    }

    private fun sign(privateKey: java.security.PrivateKey, bytes: ByteArray): ByteArray =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(privateKey)
            update(bytes)
            sign()
        }

    private fun keyPair() = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
}
