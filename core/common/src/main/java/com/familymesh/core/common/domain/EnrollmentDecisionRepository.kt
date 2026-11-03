package com.familymesh.core.common.domain

import kotlinx.coroutines.flow.Flow

data class EnrollmentDecisionRecord(
    val familySpaceId: String,
    val requestId: String,
    val applicantDeviceKeyId: String,
    val ownerDeviceKeyId: String,
    val outcome: String,
    val decidedAtEpochMillis: Long,
    val signatureBase64: String,
    val recordedAtEpochMillis: Long,
) {
    init {
        require(familySpaceId.isNotBlank())
        require(requestId.isNotBlank())
        require(applicantDeviceKeyId.isNotBlank())
        require(ownerDeviceKeyId.isNotBlank())
        require(outcome == "APPROVED" || outcome == "REJECTED")
        require(decidedAtEpochMillis >= 0L)
        require(signatureBase64.isNotBlank())
        require(recordedAtEpochMillis >= decidedAtEpochMillis)
    }
}

enum class EnrollmentDecisionPersistenceResult {
    RECORDED,
    ALREADY_RECORDED,
    REQUEST_NOT_PENDING,
    CONFLICT,
}

/** Persists only decisions that have already passed cryptographic verification. */
interface EnrollmentDecisionRepository {
    suspend fun findPendingRequest(familySpaceId: String, requestId: String): PendingEnrollmentRequest?
    suspend fun findDecision(familySpaceId: String, requestId: String): EnrollmentDecisionRecord?
    suspend fun recordDecision(
        decision: EnrollmentDecisionRecord,
    ): EnrollmentDecisionPersistenceResult
    fun observeDecisions(): Flow<List<EnrollmentDecisionRecord>>
}
