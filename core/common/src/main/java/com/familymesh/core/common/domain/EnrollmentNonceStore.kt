package com.familymesh.core.common.domain

import kotlinx.coroutines.flow.Flow

/** Durable one-time nonce consumption and local pending-request inbox. */
interface EnrollmentNonceStore {
    fun observePendingRequests(): Flow<List<PendingEnrollmentRequest>>
    /** Atomically consumes the nonce and persists the request for local review. */
    suspend fun consume(nonceDigest: String, request: PendingEnrollmentRequest): Boolean
}
