package com.familymesh.core.identity

import com.familymesh.core.common.domain.EnrollmentDecisionPersistenceResult
import com.familymesh.core.common.domain.EnrollmentDecisionRecord
import com.familymesh.core.common.domain.EnrollmentDecisionRepository
import com.familymesh.core.common.domain.InitialOwnerPinRepository
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

enum class EnrollmentDecisionAcceptanceResult {
    RECORDED,
    ALREADY_RECORDED,
    INVALID_OR_UNTRUSTED,
    REQUEST_NOT_PENDING,
    CONFLICT,
    STORAGE_UNAVAILABLE,
}

/**
 * Accepts an imported owner decision only after verifying it against the
 * locally pinned owner key and the exact request currently in the inbox.
 */
class EnrollmentDecisionAcceptanceService(
    private val pinRepository: InitialOwnerPinRepository,
    private val repository: EnrollmentDecisionRepository,
) {
    suspend fun accept(
        decision: SignedEnrollmentDecision,
        nowEpochMillis: Long,
    ): EnrollmentDecisionAcceptanceResult {
        if (nowEpochMillis < 0L) return EnrollmentDecisionAcceptanceResult.INVALID_OR_UNTRUSTED
        return try {
            // Trust is resolved at the service boundary; callers cannot supply an arbitrary owner key.
            val trustedOwner = pinRepository.observeTrustedOwners().first()
                .singleOrNull { it.familySpaceId == decision.familySpaceId }
                ?: return EnrollmentDecisionAcceptanceResult.INVALID_OR_UNTRUSTED
            val pending = repository.findPendingRequest(decision.familySpaceId, decision.requestId)
            if (pending == null) {
                val existing = repository.findDecision(decision.familySpaceId, decision.requestId)
                    ?: return EnrollmentDecisionAcceptanceResult.REQUEST_NOT_PENDING
                return if (
                    existing.familySpaceId == decision.familySpaceId &&
                    existing.requestId == decision.requestId &&
                    existing.applicantDeviceKeyId == decision.applicantDeviceKeyId &&
                    existing.ownerDeviceKeyId == decision.ownerDeviceKeyId &&
                    existing.outcome == decision.outcome.name &&
                    existing.decidedAtEpochMillis == decision.decidedAtEpochMillis &&
                    existing.signatureBase64 == Base64.getEncoder().encodeToString(decision.signature)
                ) EnrollmentDecisionAcceptanceResult.ALREADY_RECORDED
                else EnrollmentDecisionAcceptanceResult.CONFLICT
            }

            if (!EnrollmentDecisionVerifier.verify(decision, pending, trustedOwner, nowEpochMillis)) {
                return EnrollmentDecisionAcceptanceResult.INVALID_OR_UNTRUSTED
            }
            val record = EnrollmentDecisionRecord(
                familySpaceId = decision.familySpaceId,
                requestId = decision.requestId,
                applicantDeviceKeyId = decision.applicantDeviceKeyId,
                ownerDeviceKeyId = decision.ownerDeviceKeyId,
                outcome = decision.outcome.name,
                decidedAtEpochMillis = decision.decidedAtEpochMillis,
                signatureBase64 = Base64.getEncoder().encodeToString(decision.signature),
                recordedAtEpochMillis = nowEpochMillis,
            )
            return when (repository.recordDecision(record)) {
                EnrollmentDecisionPersistenceResult.RECORDED ->
                    EnrollmentDecisionAcceptanceResult.RECORDED
                EnrollmentDecisionPersistenceResult.ALREADY_RECORDED ->
                    EnrollmentDecisionAcceptanceResult.ALREADY_RECORDED
                EnrollmentDecisionPersistenceResult.REQUEST_NOT_PENDING ->
                    EnrollmentDecisionAcceptanceResult.REQUEST_NOT_PENDING
                EnrollmentDecisionPersistenceResult.CONFLICT ->
                    EnrollmentDecisionAcceptanceResult.CONFLICT
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            EnrollmentDecisionAcceptanceResult.STORAGE_UNAVAILABLE
        }
    }
}
