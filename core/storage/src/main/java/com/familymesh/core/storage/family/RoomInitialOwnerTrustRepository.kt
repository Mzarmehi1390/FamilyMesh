package com.familymesh.core.storage.family

import com.familymesh.core.common.domain.FamilyMember
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.InitialOwnerTrust
import com.familymesh.core.common.domain.InitialOwnerTrustRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room implementation for the explicit, device-local initial-owner ceremony. */
class RoomInitialOwnerTrustRepository(
    private val dao: FamilySpaceDao,
) : InitialOwnerTrustRepository {
    override fun observeInitialOwners(): Flow<List<InitialOwnerTrust>> =
        dao.observeInitialOwners().map { rows ->
            rows.map { row ->
                InitialOwnerTrust(
                    familySpaceId = row.familySpaceId,
                    ownerMemberId = row.ownerMemberId,
                    ownerDeviceKeyId = row.ownerDeviceKeyId,
                    establishedAtEpochMillis = row.establishedAtEpochMillis,
                )
            }
        }

    override suspend fun establishInitialOwner(
        familySpaceId: String,
        ownerDeviceKeyId: String,
        displayName: String,
        establishedAtEpochMillis: Long,
    ): Boolean {
        val member = FamilyMember(
            id = UUID.randomUUID().toString(),
            familySpaceId = familySpaceId.trim(),
            displayName = displayName.trim(),
            role = FamilyRole.OWNER,
            joinedAtEpochMillis = establishedAtEpochMillis,
        )
        val trust = InitialOwnerTrust(
            familySpaceId = member.familySpaceId,
            ownerMemberId = member.id,
            ownerDeviceKeyId = ownerDeviceKeyId.trim(),
            establishedAtEpochMillis = establishedAtEpochMillis,
        )
        return dao.establishInitialOwner(
            trust = InitialOwnerTrustEntity(
                familySpaceId = trust.familySpaceId,
                ownerMemberId = trust.ownerMemberId,
                ownerDeviceKeyId = trust.ownerDeviceKeyId,
                establishedAtEpochMillis = trust.establishedAtEpochMillis,
            ),
            owner = FamilyMemberEntity(
                id = member.id,
                familySpaceId = member.familySpaceId,
                displayName = member.displayName,
                role = member.role.name,
                joinedAtEpochMillis = member.joinedAtEpochMillis,
            ),
        )
    }
}
