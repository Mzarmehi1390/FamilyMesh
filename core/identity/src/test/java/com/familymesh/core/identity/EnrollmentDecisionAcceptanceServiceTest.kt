package com.familymesh.core.identity

import com.familymesh.core.common.domain.EnrollmentDecisionPersistenceResult
import com.familymesh.core.common.domain.EnrollmentDecisionRecord
import com.familymesh.core.common.domain.EnrollmentDecisionRepository
import com.familymesh.core.common.domain.InitialOwnerPinRepository
import com.familymesh.core.common.domain.InitialOwnerPinResult
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import com.familymesh.core.common.domain.TrustedInitialOwner
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EnrollmentDecisionAcceptanceServiceTest {
    private val pair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
    private val ownerKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(pair.public.encoded)
    private val trustedOwner = TrustedInitialOwner(
        familySpaceId = "family-1",
        ownerMemberId = "owner-1",
        ownerDeviceKeyId = ownerKeyId,
        ownerPublicKeyBase64 = Base64.getEncoder().encodeToString(pair.public.encoded),
        establishedAtEpochMillis = 100L,
        pinnedAtEpochMillis = 150L,
    )
    private val pending = PendingEnrollmentRequest(
        requestId = "request-1",
        familySpaceId = "family-1",
        deviceKeyId = "applicant-key",
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 200L,
        expiresAtEpochMillis = 1_000L,
        receivedAtEpochMillis = 250L,
    )

    @Test
    fun validDecisionIsPersistedAndPendingRequestIsRemoved() = runBlocking {
        val repository = FakeRepository(pending)
        val result = EnrollmentDecisionAcceptanceService(FakePinRepository(trustedOwner), repository)
            .accept(signedDecision(), 400L)

        assertEquals(EnrollmentDecisionAcceptanceResult.RECORDED, result)
        assertNull(repository.pending.value)
        assertNotNull(repository.decision.value)
        assertEquals("APPROVED", repository.decision.value?.outcome)
    }

    @Test
    fun exactReplayIsIdempotent() = runBlocking {
        val repository = FakeRepository(pending)
        val service = EnrollmentDecisionAcceptanceService(FakePinRepository(trustedOwner), repository)
        val decision = signedDecision()

        assertEquals(EnrollmentDecisionAcceptanceResult.RECORDED, service.accept(decision, 400L))
        assertEquals(EnrollmentDecisionAcceptanceResult.ALREADY_RECORDED, service.accept(decision, 401L))
    }

    @Test
    fun invalidSignatureDoesNotRemovePendingRequest() = runBlocking {
        val repository = FakeRepository(pending)
        val signed = signedDecision()
        val tampered = SignedEnrollmentDecision(
            signed.familySpaceId, signed.requestId, signed.applicantDeviceKeyId,
            signed.ownerDeviceKeyId, EnrollmentDecisionOutcome.REJECTED,
            signed.decidedAtEpochMillis, signed.signature,
        )
        val result = EnrollmentDecisionAcceptanceService(FakePinRepository(trustedOwner), repository)
            .accept(tampered, 400L)

        assertEquals(EnrollmentDecisionAcceptanceResult.INVALID_OR_UNTRUSTED, result)
        assertNotNull(repository.pending.value)
        assertNull(repository.decision.value)
    }

    @Test
    fun missingPendingRequestAndDecisionIsReported() = runBlocking {
        val repository = FakeRepository(null)
        val result = EnrollmentDecisionAcceptanceService(FakePinRepository(trustedOwner), repository)
            .accept(signedDecision(), 400L)

        assertEquals(EnrollmentDecisionAcceptanceResult.REQUEST_NOT_PENDING, result)
    }

    @Test
    fun decisionIsRejectedWhenNoOwnerIsPinnedForTheFamily() = runBlocking {
        val repository = FakeRepository(pending)
        val result = EnrollmentDecisionAcceptanceService(FakePinRepository(null), repository)
            .accept(signedDecision(), 400L)

        assertEquals(EnrollmentDecisionAcceptanceResult.INVALID_OR_UNTRUSTED, result)
        assertNotNull(repository.pending.value)
        assertNull(repository.decision.value)
    }

    @Test
    fun decisionIsRejectedWhenDifferentOwnerKeyIsPinned() = runBlocking {
        val otherPair = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }
        val otherOwner = trustedOwner.copy(
            ownerDeviceKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(otherPair.public.encoded),
            ownerPublicKeyBase64 = Base64.getEncoder().encodeToString(otherPair.public.encoded),
        )
        val repository = FakeRepository(pending)
        val result = EnrollmentDecisionAcceptanceService(FakePinRepository(otherOwner), repository)
            .accept(signedDecision(), 400L)

        assertEquals(EnrollmentDecisionAcceptanceResult.INVALID_OR_UNTRUSTED, result)
        assertNotNull(repository.pending.value)
        assertNull(repository.decision.value)
    }

    @Test
    fun ambiguousOwnerPinsAreRejected() = runBlocking {
        val repository = FakeRepository(pending)
        val pins = FakePinRepository(trustedOwner).apply {
            owners.value = listOf(trustedOwner, trustedOwner)
        }
        val result = EnrollmentDecisionAcceptanceService(pins, repository)
            .accept(signedDecision(), 400L)

        assertEquals(EnrollmentDecisionAcceptanceResult.INVALID_OR_UNTRUSTED, result)
        assertNotNull(repository.pending.value)
        assertNull(repository.decision.value)
    }

    private fun signedDecision(): SignedEnrollmentDecision {
        val unsigned = SignedEnrollmentDecision(
            pending.familySpaceId, pending.requestId, pending.deviceKeyId,
            ownerKeyId, EnrollmentDecisionOutcome.APPROVED, 300L, byteArrayOf(1),
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

    private class FakePinRepository(initial: TrustedInitialOwner?) : InitialOwnerPinRepository {
        val owners = MutableStateFlow(listOfNotNull(initial))

        override fun observeTrustedOwners(): Flow<List<TrustedInitialOwner>> = owners

        override suspend fun pinInitialOwner(
            familySpaceId: String,
            ownerMemberId: String,
            ownerDeviceKeyId: String,
            ownerPublicKeyBase64: String,
            establishedAtEpochMillis: Long,
            confirmedFingerprint: String,
            pinnedAtEpochMillis: Long,
        ): InitialOwnerPinResult = error("Pinning is not used by this test fake.")
    }

    private class FakeRepository(initial: PendingEnrollmentRequest?) : EnrollmentDecisionRepository {
        val pending = MutableStateFlow(initial)
        val decision = MutableStateFlow<EnrollmentDecisionRecord?>(null)

        override suspend fun findPendingRequest(familySpaceId: String, requestId: String) =
            pending.value?.takeIf { it.familySpaceId == familySpaceId && it.requestId == requestId }

        override suspend fun findDecision(familySpaceId: String, requestId: String) =
            decision.value?.takeIf { it.familySpaceId == familySpaceId && it.requestId == requestId }

        override suspend fun recordDecision(decisionRecord: EnrollmentDecisionRecord): EnrollmentDecisionPersistenceResult {
            val existing = decision.value
            if (existing != null) return if (existing == decisionRecord) {
                EnrollmentDecisionPersistenceResult.ALREADY_RECORDED
            } else EnrollmentDecisionPersistenceResult.CONFLICT
            val current = pending.value ?: return EnrollmentDecisionPersistenceResult.REQUEST_NOT_PENDING
            if (current.familySpaceId != decisionRecord.familySpaceId ||
                current.requestId != decisionRecord.requestId ||
                current.deviceKeyId != decisionRecord.applicantDeviceKeyId
            ) return EnrollmentDecisionPersistenceResult.CONFLICT
            decision.value = decisionRecord
            pending.value = null
            return EnrollmentDecisionPersistenceResult.RECORDED
        }

        override fun observeDecisions(): Flow<List<EnrollmentDecisionRecord>> =
            MutableStateFlow(listOfNotNull(decision.value))
    }
}
