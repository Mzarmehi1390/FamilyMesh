package com.familymesh.core.common.domain

import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for family-space domain records.
 *
 * Implementations belong in a data module. This contract does not imply that
 * remote enrollment, authorization, invitations, or synchronization exist.
 */
interface FamilySpaceRepository {
    fun observeFamilySpaces(): Flow<List<FamilySpace>>
    fun observeMembers(familySpaceId: String): Flow<List<FamilyMember>>

    suspend fun createFamilySpace(name: String, createdAtEpochMillis: Long): FamilySpace

    /** Adds a local member-list entry only; this does not authenticate or authorize a device. */
    suspend fun addLocalMember(
        familySpaceId: String,
        displayName: String,
        joinedAtEpochMillis: Long
    ): FamilyMember
}
