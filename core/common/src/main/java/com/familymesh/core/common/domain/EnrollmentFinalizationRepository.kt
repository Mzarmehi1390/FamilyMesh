package com.familymesh.core.common.domain

import kotlinx.coroutines.flow.Flow

/** Verified applicant device key accepted by the locally anchored family owner. */
data class AuthenticatedFamilyDevice(
    val familySpaceId: String,
    val requestId: String,
    val deviceKeyId: String,
    val publicKeyBase64: String,
    val role: FamilyRole,
    val joinedAtEpochMillis: Long,
    val acceptanceSignatureBase64: String,
) {
    init {
        require(familySpaceId.isNotBlank() && requestId.isNotBlank() && deviceKeyId.isNotBlank())
        require(publicKeyBase64.isNotBlank() && acceptanceSignatureBase64.isNotBlank())
        require(joinedAtEpochMillis >= 0L)
    }
}

enum class EnrollmentFinalizationResult { FINALIZED, ALREADY_FINALIZED, REQUEST_NOT_PENDING, CONFLICT }

/** Atomic owner-side membership credential insertion and pending-request consumption. */
interface EnrollmentFinalizationRepository {
    fun observeAuthenticatedDevices(familySpaceId: String): Flow<List<AuthenticatedFamilyDevice>>
    suspend fun findPendingRequest(familySpaceId: String, requestId: String): PendingEnrollmentRequest?
    suspend fun findFinalized(
        familySpaceId: String,
        requestId: String,
        deviceKeyId: String,
        acceptanceSignatureBase64: String,
    ): EnrollmentFinalizationResult
    suspend fun finalizeEnrollment(device: AuthenticatedFamilyDevice): EnrollmentFinalizationResult
}
