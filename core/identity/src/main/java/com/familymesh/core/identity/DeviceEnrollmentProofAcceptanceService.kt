package com.familymesh.core.identity

import com.familymesh.core.common.domain.EnrollmentNonceStore
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CancellationException

sealed interface EnrollmentProofAcceptanceResult {
    data object Accepted : EnrollmentProofAcceptanceResult
    data object InvalidOrExpired : EnrollmentProofAcceptanceResult
    data object ReplayDetected : EnrollmentProofAcceptanceResult
    data object NonceStoreUnavailable : EnrollmentProofAcceptanceResult
}

/** Verifies a proof and atomically consumes its nonce; does not grant membership. */
class DeviceEnrollmentProofAcceptanceService(
    private val nonceStore: EnrollmentNonceStore,
) {
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
            if (nonceStore.consume(
                    nonceDigest = digest,
                    familySpaceId = proof.familySpaceId,
                    requestId = proof.requestId,
                    consumedAtEpochMillis = nowEpochMillis,
                    expiresAtEpochMillis = proof.expiresAtEpochMillis,
                )
            ) EnrollmentProofAcceptanceResult.Accepted
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
