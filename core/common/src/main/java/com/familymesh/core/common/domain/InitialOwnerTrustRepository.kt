package com.familymesh.core.common.domain

import kotlinx.coroutines.flow.Flow

/** Stores the single local initial-owner trust anchor for each family space. */
interface InitialOwnerTrustRepository {
    fun observeInitialOwners(): Flow<List<InitialOwnerTrust>>
    /** Returns false if a family already has an initial owner. */
    suspend fun establishInitialOwner(
        familySpaceId: String,
        ownerDeviceKeyId: String,
        displayName: String,
        establishedAtEpochMillis: Long,
    ): Boolean
}
