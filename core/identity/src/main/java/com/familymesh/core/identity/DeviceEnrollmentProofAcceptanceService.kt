package com.familymesh.core.identity

import com.familymesh.core.common.domain.EnrollmentNonceStore
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CancellationException

sealed interface EnrollmentProofAcceptanceResult {
    data object Accepted : EnrollmentProofAcceptanceResult
    data object InvalidOrExpired : EnrollmentProofAcceptanceResult
    data object ReplayDetected : EnrollmentProofAcceptanceResult
    data object NonceStoreUnavailable : EnrollmentProofAcceptanceResult
}

/** Verifies a proof, atomically consumes its nonce, and stores it for review. */
class DeviceEnrollmentProofAcceptanceService(
    private val nonceStore: EnrollmentNonceStore,
) {
    fun observePendingRequests() = nonceStore.observePendingRequests()

    suspend fun accept(
        proof: SignedDeviceEnrollmentRequest,
        identity: DevicePublicIdentity,
        nowEpochMillis: Long,
    ): EnrollmentProofAcceptanceResult {
        if (!DeviceEnrollmentProofVerifier.verify(proof, identity, nowEpochMillis)) {
            return EnrollmentProofAcceptanceResult.InvalidOrExpired
        }
        val digest = MessageDigest.getInstance("SHA-256").run {
            update(NONCE_DOMAIN)
            digest(proof.nonce)
        }.joinToString("") { String.format(Locale.ROOT, "%02x", it.toInt() and 0xff) }

        return try {
            val pending = PendingEnrollmentRequest(
                requestId = proof.requestId,
                familySpaceId = proof.familySpaceId,
                deviceKeyId = proof.deviceKeyId,
                requestedRole = proof.requestedRole,
                createdAtEpochMillis = proof.createdAtEpochMillis,
                expiresAtEpochMillis = proof.expiresAtEpochMillis,
                receivedAtEpochMillis = nowEpochMillis,
            )
            if (nonceStore.consume(digest, pending)) EnrollmentProofAcceptanceResult.Accepted
            else EnrollmentProofAcceptanceResult.ReplayDetected
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            EnrollmentProofAcceptanceResult.NonceStoreUnavailable
        }
    }

    private companion object {
        val NONCE_DOMAIN = "FamilyMesh:enrollment-nonce:v1\u0000".toByteArray(Charsets.UTF_8)
    }
}
