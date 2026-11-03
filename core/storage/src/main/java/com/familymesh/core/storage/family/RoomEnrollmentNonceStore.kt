package com.familymesh.core.storage.family

import com.familymesh.core.common.domain.EnrollmentNonceStore
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed atomic one-time nonce storage and pending enrollment inbox. */
class RoomEnrollmentNonceStore(
    private val dao: EnrollmentNonceDao,
) : EnrollmentNonceStore {
    override fun observePendingRequests(): Flow<List<PendingEnrollmentRequest>> =
        dao.observePendingRequests().map { rows -> rows.map(PendingEnrollmentRequestEntity::toDomain) }

    override suspend fun consume(nonceDigest: String, request: PendingEnrollmentRequest): Boolean {
        require(nonceDigest.matches(Regex("[0-9a-f]{64}")))
        return dao.consumeNonce(
            nonce = ConsumedEnrollmentNonceEntity(
                nonceDigest = nonceDigest,
                familySpaceId = request.familySpaceId,
                requestId = request.requestId,
                consumedAtEpochMillis = request.receivedAtEpochMillis,
                expiresAtEpochMillis = request.expiresAtEpochMillis,
            ),
            pending = PendingEnrollmentRequestEntity(
                requestId = request.requestId,
                familySpaceId = request.familySpaceId,
                deviceKeyId = request.deviceKeyId,
                requestedRole = request.requestedRole.name,
                createdAtEpochMillis = request.createdAtEpochMillis,
                expiresAtEpochMillis = request.expiresAtEpochMillis,
                receivedAtEpochMillis = request.receivedAtEpochMillis,
            ),
            nowEpochMillis = request.receivedAtEpochMillis,
        )
    }
}

private fun PendingEnrollmentRequestEntity.toDomain() = PendingEnrollmentRequest(
    requestId = requestId,
    familySpaceId = familySpaceId,
    deviceKeyId = deviceKeyId,
    requestedRole = FamilyRole.valueOf(requestedRole),
    createdAtEpochMillis = createdAtEpochMillis,
    expiresAtEpochMillis = expiresAtEpochMillis,
    receivedAtEpochMillis = receivedAtEpochMillis,
)
