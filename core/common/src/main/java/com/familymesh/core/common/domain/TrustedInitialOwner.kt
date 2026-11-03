package com.familymesh.core.common.domain

data class TrustedInitialOwner(
    val familySpaceId: String,
    val ownerMemberId: String,
    val ownerDeviceKeyId: String,
    val ownerPublicKeyBase64: String,
    val establishedAtEpochMillis: Long,
    val pinnedAtEpochMillis: Long,
) {
    init {
        require(familySpaceId.isNotBlank() && ownerMemberId.isNotBlank())
        require(ownerDeviceKeyId.isNotBlank() && ownerPublicKeyBase64.isNotBlank())
        require(establishedAtEpochMillis >= 0L && pinnedAtEpochMillis >= establishedAtEpochMillis)
    }
}

enum class InitialOwnerPinResult { PINNED, ALREADY_PINNED, CONFLICT }

interface InitialOwnerPinRepository {
    fun observeTrustedOwners(): kotlinx.coroutines.flow.Flow<List<TrustedInitialOwner>>
    suspend fun pinInitialOwner(
        familySpaceId: String,
        ownerMemberId: String,
        ownerDeviceKeyId: String,
        ownerPublicKeyBase64: String,
        establishedAtEpochMillis: Long,
        confirmedFingerprint: String,
        pinnedAtEpochMillis: Long,
    ): InitialOwnerPinResult
}
