package com.familymesh.core.storage.family

import android.util.Base64
import com.familymesh.core.common.domain.InitialOwnerPinRepository
import com.familymesh.core.common.domain.InitialOwnerPinResult
import com.familymesh.core.common.domain.TrustedInitialOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomInitialOwnerPinRepository(private val dao: FamilySpaceDao) : InitialOwnerPinRepository {
    override fun observeTrustedOwners(): Flow<List<TrustedInitialOwner>> =
        dao.observeTrustedOwners().map { rows -> rows.map { row ->
            TrustedInitialOwner(row.familySpaceId, row.ownerMemberId, row.ownerDeviceKeyId,
                row.ownerPublicKeyBase64, row.establishedAtEpochMillis, row.pinnedAtEpochMillis)
        } }

    override suspend fun pinInitialOwner(
        familySpaceId: String,
        ownerMemberId: String,
        ownerDeviceKeyId: String,
        ownerPublicKeyBase64: String,
        establishedAtEpochMillis: Long,
        confirmedFingerprint: String,
        pinnedAtEpochMillis: Long,
    ): InitialOwnerPinResult {
        require(confirmedFingerprint.trim().equals(ownerDeviceKeyId, ignoreCase = true))
        require(familySpaceId.isNotBlank() && ownerMemberId.isNotBlank() && ownerDeviceKeyId.isNotBlank())
        require(establishedAtEpochMillis >= 0L && pinnedAtEpochMillis >= establishedAtEpochMillis)
        val key = Base64.decode(ownerPublicKeyBase64, Base64.NO_WRAP)
        require(key.isNotEmpty() && key.size <= 1024)
        val entity = TrustedInitialOwnerEntity(familySpaceId, ownerMemberId, ownerDeviceKeyId,
            ownerPublicKeyBase64, establishedAtEpochMillis, pinnedAtEpochMillis)
        if (dao.insertTrustedOwner(entity) != -1L) return InitialOwnerPinResult.PINNED
        val old = dao.getTrustedOwner(familySpaceId) ?: return InitialOwnerPinResult.CONFLICT
        return if (old.ownerMemberId == ownerMemberId && old.ownerDeviceKeyId == ownerDeviceKeyId &&
            old.ownerPublicKeyBase64 == ownerPublicKeyBase64 &&
            old.establishedAtEpochMillis == establishedAtEpochMillis) InitialOwnerPinResult.ALREADY_PINNED
        else InitialOwnerPinResult.CONFLICT
    }
}
