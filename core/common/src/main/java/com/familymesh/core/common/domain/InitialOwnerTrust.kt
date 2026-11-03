package com.familymesh.core.common.domain

/** Local record of the explicit initial-owner trust ceremony. */
data class InitialOwnerTrust(
    val familySpaceId: String,
    val ownerMemberId: String,
    val ownerDeviceKeyId: String,
    val establishedAtEpochMillis: Long,
) {
    init {
        require(familySpaceId.isNotBlank())
        require(ownerMemberId.isNotBlank())
        require(ownerDeviceKeyId.isNotBlank())
        require(establishedAtEpochMillis >= 0L)
    }
}
