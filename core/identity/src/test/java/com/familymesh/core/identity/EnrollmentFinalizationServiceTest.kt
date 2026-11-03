package com.familymesh.core.identity

import com.familymesh.core.common.domain.AuthenticatedFamilyDevice
import com.familymesh.core.common.domain.EnrollmentFinalizationRepository
import com.familymesh.core.common.domain.EnrollmentFinalizationResult
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.InitialOwnerTrust
import com.familymesh.core.common.domain.InitialOwnerTrustRepository
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrollmentFinalizationServiceTest {
    private val ownerPair = keyPair()
    private val applicantPair = keyPair()
    private val ownerKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(ownerPair.public.encoded)
    private val applicantKeyId = DeviceKeyFingerprint.fromEncodedPublicKey(applicantPair.public.encoded)
    private val request = PendingEnrollmentRequest(
        requestId = "request-1",
        familySpaceId = "family-1",
        deviceKeyId = applicantKeyId,
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 200L,
        expiresAtEpochMillis = 1_000L,
        receivedAtEpochMillis = 250L,
    )
    private val anchor = InitialOwnerTrust(
        familySpaceId = "family-1",
        ownerMemberId = "owner-1",
        ownerDeviceKeyId = ownerKeyId,
        establishedAtEpochMillis = 100L,
    )

    @Test
    fun validAcceptanceCreatesAuthenticatedDeviceAndConsumesPendingRequest() = runBlocking {
        val repository = FakeFinalizationRepository(request)
        val service = service(repository)

        assertEquals(EnrollmentFinalizationOutcome.Finalized, service.finalize(acceptance(), 500L))
        val stored = repository.finalized
        assertNotNull(stored)
        assertEquals(applicantKeyId, stored?.deviceKeyId)
        assertEquals(Base64.getEncoder().encodeToString(applicantPair.public.encoded), stored?.publicKeyBase64)
        assertEquals(FamilyRole.MEMBER, stored?.role)
        assertEquals(400L, stored?.joinedAtEpochMillis)
        assertNull(repository.pending)
    }

    @Test
    fun exactReplayAfterFinalizationIsIdempotent() = runBlocking {
        val repository = FakeFinalizationRepository(request)
        val service = service(repository)
        val signed = acceptance()

        assertEquals(EnrollmentFinalizationOutcome.Finalized, service.finalize(signed, 500L))
        assertEquals(EnrollmentFinalizationOutcome.AlreadyFinalized, service.finalize(signed, 501L))
    }

    @Test
    fun tamperedAcceptanceDoesNotCreateMembershipOrConsumeRequest() = runBlocking {
        val repository = FakeFinalizationRepository(request)
        val valid = acceptance()
        val tampered = SignedEnrollmentAcceptance(
            valid.decision,
            valid.applicantDeviceKeyId,
            valid.applicantPublicKeyBytes,
            valid.acceptedAtEpochMillis,
            valid.signature.reversedArray(),
        )

        assertEquals(EnrollmentFinalizationOutcome.InvalidOrUntrusted, service(repository).finalize(tampered, 500L))
        assertNull(repository.finalized)
        assertNotNull(repository.pending)
    }

    @Test
    fun deviceThatDoesNotMatchLocalInitialOwnerAnchorCannotFinalize() = runBlocking {
        val repository = FakeFinalizationRepository(request)
        val otherOwner = keyPair()
        val wrongManager = FakeKeyManager(DevicePublicIdentity(
            DeviceKeyFingerprint.fromEncodedPublicKey(otherOwner.public.encoded),
            otherOwner.public.encoded,
            100L,
        ))

        assertEquals(
            EnrollmentFinalizationOutcome.InvalidOrUntrusted,
            service(repository, wrongManager).finalize(acceptance(), 500L),
        )
        assertNull(repository.finalized)
        assertNotNull(repository.pending)
    }

    @Test
    fun conflictingStoredFinalizationIsNotReportedAsSuccess() = runBlocking {
        val repository = FakeFinalizationRepository(request)
        repository.finalized = AuthenticatedFamilyDevice(
            familySpaceId = request.familySpaceId,
            requestId = request.requestId,
            deviceKeyId = applicantKeyId,
            publicKeyBase64 = "different-key",
            role = FamilyRole.MEMBER,
            joinedAtEpochMillis = 400L,
            acceptanceSignatureBase64 = "different-signature",
        )
        repository.pending = null

        assertEquals(EnrollmentFinalizationOutcome.Conflict, service(repository).finalize(acceptance(), 500L))
    }

    private fun service(
        repository: FakeFinalizationRepository,
        keyManager: DeviceKeyManager = FakeKeyManager(
            DevicePublicIdentity(ownerKeyId, ownerPair.public.encoded, 100L),
        ),
    ) = EnrollmentFinalizationService(
        trustRepository = FakeTrustRepository(anchor),
        keyManager = keyManager,
        repository = repository,
    )

    private suspend fun acceptance(): SignedEnrollmentAcceptance {
        val decisionUnsigned = SignedEnrollmentDecision(
            request.familySpaceId,
            request.requestId,
            applicantKeyId,
            ownerKeyId,
            EnrollmentDecisionOutcome.APPROVED,
            300L,
            byteArrayOf(1),
        )
        val decision = SignedEnrollmentDecision(
            decisionUnsigned.familySpaceId,
            decisionUnsigned.requestId,
            decisionUnsigned.applicantDeviceKeyId,
            decisionUnsigned.ownerDeviceKeyId,
            decisionUnsigned.outcome,
            decisionUnsigned.decidedAtEpochMillis,
            sign(ownerPair, decisionUnsigned.signingBytes()),
        )
        val applicantIdentity = DevicePublicIdentity(applicantKeyId, applicantPair.public.encoded, 200L)
        return EnrollmentAcceptanceSigner(DeviceMessageSigner { sign(applicantPair, it) })
            .sign(decision, applicantIdentity, 400L)
    }

    private class FakeTrustRepository(private val owner: InitialOwnerTrust) : InitialOwnerTrustRepository {
        override fun observeInitialOwners(): Flow<List<InitialOwnerTrust>> = MutableStateFlow(listOf(owner))
        override suspend fun establishInitialOwner(
            familySpaceId: String,
            ownerDeviceKeyId: String,
            displayName: String,
            establishedAtEpochMillis: Long,
        ) = false
    }

    private class FakeKeyManager(private val identity: DevicePublicIdentity) : DeviceKeyManager {
        override suspend fun getPublicIdentity() = identity
        override suspend fun createPublicIdentity() = error("Not used in this test")
    }

    private class FakeFinalizationRepository(initial: PendingEnrollmentRequest?) : EnrollmentFinalizationRepository {
        var pending: PendingEnrollmentRequest? = initial
        var finalized: AuthenticatedFamilyDevice? = null

        override suspend fun findPendingRequest(familySpaceId: String, requestId: String) =
            pending?.takeIf { it.familySpaceId == familySpaceId && it.requestId == requestId }

        override suspend fun findFinalized(
            familySpaceId: String,
            requestId: String,
            deviceKeyId: String,
            acceptanceSignatureBase64: String,
        ): EnrollmentFinalizationResult {
            val existing = finalized ?: return EnrollmentFinalizationResult.REQUEST_NOT_PENDING
            return if (
                existing.familySpaceId == familySpaceId &&
                existing.requestId == requestId &&
                existing.deviceKeyId == deviceKeyId &&
                existing.acceptanceSignatureBase64 == acceptanceSignatureBase64
            ) EnrollmentFinalizationResult.ALREADY_FINALIZED else EnrollmentFinalizationResult.CONFLICT
        }

        override suspend fun finalizeEnrollment(device: AuthenticatedFamilyDevice): EnrollmentFinalizationResult {
            val existing = finalized
            if (existing != null) return if (existing == device) {
                EnrollmentFinalizationResult.ALREADY_FINALIZED
            } else EnrollmentFinalizationResult.CONFLICT
            val current = pending ?: return EnrollmentFinalizationResult.REQUEST_NOT_PENDING
            if (
                current.familySpaceId != device.familySpaceId ||
                current.requestId != device.requestId ||
                current.deviceKeyId != device.deviceKeyId ||
                current.requestedRole != device.role ||
                device.joinedAtEpochMillis !in current.createdAtEpochMillis until current.expiresAtEpochMillis
            ) return EnrollmentFinalizationResult.CONFLICT
            finalized = device
            pending = null
            return EnrollmentFinalizationResult.FINALIZED
        }
    }

    private fun keyPair(): KeyPair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }

    private fun sign(pair: KeyPair, bytes: ByteArray): ByteArray =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(pair.private)
            update(bytes)
            sign()
        }
}
