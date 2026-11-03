package com.familymesh.core.storage.family

import com.familymesh.core.common.domain.EnrollmentDecisionPersistenceResult
import com.familymesh.core.common.domain.EnrollmentDecisionRecord
import com.familymesh.core.common.domain.EnrollmentDecisionRepository
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEnrollmentDecisionRepository(
    private val dao: EnrollmentNonceDao,
) : EnrollmentDecisionRepository {
    override suspend fun findPendingRequest(
        familySpaceId: String,
        requestId: String,
    ): PendingEnrollmentRequest? = dao.getPendingRequest(familySpaceId, requestId)?.toDomain()

    override suspend fun findDecision(
        familySpaceId: String,
        requestId: String,
    ): EnrollmentDecisionRecord? = dao.getDecision(familySpaceId, requestId)?.toDomain()

    override suspend fun recordDecision(
        decision: EnrollmentDecisionRecord,
    ): EnrollmentDecisionPersistenceResult {
        require(decision.signatureBase64.length <= MAX_SIGNATURE_BASE64_LENGTH)
        require(decision.signatureBase64.matches(Regex("[A-Za-z0-9+/]+={0,2}")))
        val entity = EnrollmentDecisionEntity(
            familySpaceId = decision.familySpaceId,
            requestId = decision.requestId,
            applicantDeviceKeyId = decision.applicantDeviceKeyId,
            ownerDeviceKeyId = decision.ownerDeviceKeyId,
            outcome = decision.outcome,
            decidedAtEpochMillis = decision.decidedAtEpochMillis,
            signatureBase64 = decision.signatureBase64,
            recordedAtEpochMillis = decision.recordedAtEpochMillis,
        )
        return when (dao.persistDecision(entity, decision.recordedAtEpochMillis)) {
            "RECORDED" -> EnrollmentDecisionPersistenceResult.RECORDED
            "ALREADY_RECORDED" -> EnrollmentDecisionPersistenceResult.ALREADY_RECORDED
            "REQUEST_NOT_PENDING" -> EnrollmentDecisionPersistenceResult.REQUEST_NOT_PENDING
            else -> EnrollmentDecisionPersistenceResult.CONFLICT
        }
    }

    override fun observeDecisions(): Flow<List<EnrollmentDecisionRecord>> =
        dao.observeDecisions().map { rows -> rows.map { it.toDomain() } }

    private fun PendingEnrollmentRequestEntity.toDomain() = PendingEnrollmentRequest(
        requestId = requestId,
        familySpaceId = familySpaceId,
        deviceKeyId = deviceKeyId,
        requestedRole = FamilyRole.valueOf(requestedRole),
        createdAtEpochMillis = createdAtEpochMillis,
        expiresAtEpochMillis = expiresAtEpochMillis,
        receivedAtEpochMillis = receivedAtEpochMillis,
    )

    private fun EnrollmentDecisionEntity.toDomain() = EnrollmentDecisionRecord(
        familySpaceId = familySpaceId,
        requestId = requestId,
        applicantDeviceKeyId = applicantDeviceKeyId,
        ownerDeviceKeyId = ownerDeviceKeyId,
        outcome = outcome,
        decidedAtEpochMillis = decidedAtEpochMillis,
        signatureBase64 = signatureBase64,
        recordedAtEpochMillis = recordedAtEpochMillis,
    )

    private companion object {
        const val MAX_SIGNATURE_BASE64_LENGTH = 512
    }
}
