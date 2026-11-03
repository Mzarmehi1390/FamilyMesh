package com.familymesh.core.identity

import com.familymesh.core.common.domain.DeviceEnrollmentRequest
import com.familymesh.core.common.domain.EnrollmentNonceStore
import com.familymesh.core.common.domain.FamilyRole
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceEnrollmentProofAcceptanceServiceTest {
    private val pair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
    private val identity = DevicePublicIdentity(
        keyId = DeviceKeyFingerprint.fromEncodedPublicKey(pair.public.encoded),
        publicKeyBytes = pair.public.encoded,
        createdAtEpochMillis = 1_000L,
    )
    private val signer = DeviceMessageSigner { message ->
        Signature.getInstance("SHA256withECDSA").run {
            initSign(pair.private)
            update(message)
            sign()
        }
    }

    @Test
    fun firstValidProofIsAcceptedAndSecondUseIsRejected() = runBlocking {
        val store = InMemoryNonceStore()
        val service = DeviceEnrollmentProofAcceptanceService(store)
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)
        assertEquals(EnrollmentProofAcceptanceResult.Accepted, service.accept(proof, identity, 1_500L))
        assertEquals(EnrollmentProofAcceptanceResult.ReplayDetected, service.accept(proof, identity, 1_500L))
        assertEquals(1, store.size)
    }

    @Test
    fun nonceStoreFailureNeverAcceptsProof() = runBlocking {
        val failingStore = object : EnrollmentNonceStore {
            override suspend fun consume(
                nonceDigest: String,
                familySpaceId: String,
                requestId: String,
                consumedAtEpochMillis: Long,
                expiresAtEpochMillis: Long,
            ): Boolean = error("database unavailable")
        }
        val service = DeviceEnrollmentProofAcceptanceService(failingStore)
        val proof = DeviceEnrollmentProofSigner(signer).sign(request(), identity)
        assertEquals(
            EnrollmentProofAcceptanceResult.NonceStoreUnavailable,
            service.accept(proof, identity, 1_500L),
        )
    }

    private fun request() = DeviceEnrollmentRequest(
        id = "request-accept-1",
        familySpaceId = "family-1",
        deviceKeyId = identity.keyId,
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 1_000L,
        expiresAtEpochMillis = 2_000L,
    )

    private class InMemoryNonceStore : EnrollmentNonceStore {
        private val consumed = mutableSetOf<String>()
        val size: Int get() = consumed.size
        override suspend fun consume(
            nonceDigest: String,
            familySpaceId: String,
            requestId: String,
            consumedAtEpochMillis: Long,
            expiresAtEpochMillis: Long,
        ): Boolean = consumed.add(nonceDigest)
    }
}
