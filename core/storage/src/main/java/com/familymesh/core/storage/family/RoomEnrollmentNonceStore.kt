package com.familymesh.core.storage.family

import com.familymesh.core.common.domain.EnrollmentNonceStore

/** Room-backed, atomic one-time nonce storage for enrollment proofs. */
class RoomEnrollmentNonceStore(
    private val dao: EnrollmentNonceDao,
) : EnrollmentNonceStore {
    override suspend fun consume(
        nonceDigest: String,
        familySpaceId: String,
        requestId: String,
        consumedAtEpochMillis: Long,
        expiresAtEpochMillis: Long,
    ): Boolean {
        require(nonceDigest.matches(Regex("[0-9a-f]{64}")))
        require(familySpaceId.isNotBlank() && requestId.isNotBlank())
        require(consumedAtEpochMillis >= 0L && expiresAtEpochMillis > consumedAtEpochMillis)
        return dao.consumeNonce(
            entity = ConsumedEnrollmentNonceEntity(
                nonceDigest = nonceDigest,
                familySpaceId = familySpaceId,
                requestId = requestId,
                consumedAtEpochMillis = consumedAtEpochMillis,
                expiresAtEpochMillis = expiresAtEpochMillis,
            ),
            nowEpochMillis = consumedAtEpochMillis,
        )
    }
}
