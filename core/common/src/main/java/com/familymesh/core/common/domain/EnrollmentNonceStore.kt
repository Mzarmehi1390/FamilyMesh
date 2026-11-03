package com.familymesh.core.common.domain

/** Durable one-time nonce consumption for signed enrollment proofs. */
interface EnrollmentNonceStore {
    suspend fun consume(
        nonceDigest: String,
        familySpaceId: String,
        requestId: String,
        consumedAtEpochMillis: Long,
        expiresAtEpochMillis: Long,
    ): Boolean
}
