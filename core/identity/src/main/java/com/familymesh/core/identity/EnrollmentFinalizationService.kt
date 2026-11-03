package com.familymesh.core.identity

import com.familymesh.core.common.domain.AuthenticatedFamilyDevice
import com.familymesh.core.common.domain.EnrollmentFinalizationRepository
import com.familymesh.core.common.domain.EnrollmentFinalizationResult
import com.familymesh.core.common.domain.InitialOwnerTrustRepository
import com.familymesh.core.common.domain.TrustedInitialOwner
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface EnrollmentFinalizationOutcome {
    data object Finalized : EnrollmentFinalizationOutcome
    data object AlreadyFinalized : EnrollmentFinalizationOutcome
    data object InvalidOrUntrusted : EnrollmentFinalizationOutcome
    data object RequestNotPending : EnrollmentFinalizationOutcome
    data object Conflict : EnrollmentFinalizationOutcome
    data object StorageUnavailable : EnrollmentFinalizationOutcome
}

/** Owner-side finalization after checking the original request and both signatures. */
class EnrollmentFinalizationService(
    private val trustRepository: InitialOwnerTrustRepository,
    private val keyManager: DeviceKeyManager,
    private val repository: EnrollmentFinalizationRepository,
) {
    suspend fun finalize(
        acceptance: SignedEnrollmentAcceptance,
        nowEpochMillis: Long,
    ): EnrollmentFinalizationOutcome {
        if (nowEpochMillis < 0L) return EnrollmentFinalizationOutcome.InvalidOrUntrusted
        try {
            val familyId = acceptance.decision.familySpaceId
            val requestId = acceptance.decision.requestId
            val signatureBase64 = Base64.getEncoder().encodeToString(acceptance.signature)
            val request = repository.findPendingRequest(familyId, requestId)
                ?: return mapResult(repository.findFinalized(
                    familyId, requestId, acceptance.applicantDeviceKeyId, signatureBase64,
                ))

            val anchor = trustRepository.observeInitialOwners().first()
                .singleOrNull { it.familySpaceId == request.familySpaceId }
                ?: return EnrollmentFinalizationOutcome.InvalidOrUntrusted
            val identity = keyManager.getPublicIdentity()
                ?: return EnrollmentFinalizationOutcome.InvalidOrUntrusted
            if (identity.keyId != anchor.ownerDeviceKeyId ||
                DeviceKeyFingerprint.fromEncodedPublicKey(identity.publicKeyBytes) != identity.keyId
            ) return EnrollmentFinalizationOutcome.InvalidOrUntrusted

            val trustedOwner = TrustedInitialOwner(
                familySpaceId = anchor.familySpaceId,
                ownerMemberId = anchor.ownerMemberId,
                ownerDeviceKeyId = anchor.ownerDeviceKeyId,
                ownerPublicKeyBase64 = Base64.getEncoder().encodeToString(identity.publicKeyBytes),
                establishedAtEpochMillis = anchor.establishedAtEpochMillis,
                pinnedAtEpochMillis = anchor.establishedAtEpochMillis,
            )
            if (!EnrollmentAcceptanceVerifier.verify(acceptance, request, trustedOwner, nowEpochMillis)) {
                return EnrollmentFinalizationOutcome.InvalidOrUntrusted
            }

            return mapResult(repository.finalizeEnrollment(
                AuthenticatedFamilyDevice(
                    familySpaceId = request.familySpaceId,
                    requestId = request.requestId,
                    deviceKeyId = request.deviceKeyId,
                    publicKeyBase64 = Base64.getEncoder().encodeToString(acceptance.applicantPublicKeyBytes),
                    role = request.requestedRole,
                    joinedAtEpochMillis = acceptance.acceptedAtEpochMillis,
                    acceptanceSignatureBase64 = signatureBase64,
                )
            ))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return EnrollmentFinalizationOutcome.StorageUnavailable
        }
    }

    private fun mapResult(result: EnrollmentFinalizationResult): EnrollmentFinalizationOutcome =
        when (result) {
            EnrollmentFinalizationResult.FINALIZED -> EnrollmentFinalizationOutcome.Finalized
            EnrollmentFinalizationResult.ALREADY_FINALIZED -> EnrollmentFinalizationOutcome.AlreadyFinalized
            EnrollmentFinalizationResult.REQUEST_NOT_PENDING -> EnrollmentFinalizationOutcome.RequestNotPending
            EnrollmentFinalizationResult.CONFLICT -> EnrollmentFinalizationOutcome.Conflict
        }
}
