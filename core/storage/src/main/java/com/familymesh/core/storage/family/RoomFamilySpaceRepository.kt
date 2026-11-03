package com.familymesh.core.storage.family

import com.familymesh.core.common.domain.FamilyMember
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.FamilySpace
import com.familymesh.core.common.domain.FamilySpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomFamilySpaceRepository(
    private val dao: FamilySpaceDao
) : FamilySpaceRepository {
    override fun observeFamilySpaces(): Flow<List<FamilySpace>> =
        dao.observeFamilySpaces().map { records -> records.map(FamilySpaceEntity::toDomain) }

    override fun observeMembers(familySpaceId: String): Flow<List<FamilyMember>> =
        dao.observeMembers(familySpaceId).map { records -> records.map(FamilyMemberEntity::toDomain) }

    override suspend fun createFamilySpace(
        name: String,
        createdAtEpochMillis: Long
    ): FamilySpace {
        val familySpace = FamilySpace(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAtEpochMillis = createdAtEpochMillis
        )
        dao.insertFamilySpace(
            FamilySpaceEntity(
                id = familySpace.id,
                name = familySpace.name,
                createdAtEpochMillis = familySpace.createdAtEpochMillis
            )
        )
        return familySpace
    }

    override suspend fun addLocalMember(
        familySpaceId: String,
        displayName: String,
        joinedAtEpochMillis: Long
    ): FamilyMember {
        val member = FamilyMember(
            id = UUID.randomUUID().toString(),
            familySpaceId = familySpaceId.trim(),
            displayName = displayName.trim(),
            role = FamilyRole.MEMBER,
            joinedAtEpochMillis = joinedAtEpochMillis
        )
        dao.insertFamilyMember(
            FamilyMemberEntity(
                id = member.id,
                familySpaceId = member.familySpaceId,
                displayName = member.displayName,
                role = member.role.name,
                joinedAtEpochMillis = member.joinedAtEpochMillis
            )
        )
        return member
    }
}

private fun FamilySpaceEntity.toDomain() = FamilySpace(id, name, createdAtEpochMillis)

private fun FamilyMemberEntity.toDomain() = FamilyMember(
    id = id,
    familySpaceId = familySpaceId,
    displayName = displayName,
    role = FamilyRole.valueOf(role),
    joinedAtEpochMillis = joinedAtEpochMillis
)
