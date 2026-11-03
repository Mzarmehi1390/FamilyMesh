package com.familymesh.core.common.domain

/** A verified device-key request awaiting a separate trusted-owner decision. */
data class PendingEnrollmentRequest(
    val requestId: String,
    val familySpaceId: String,
    val deviceKeyId: String,
    val requestedRole: FamilyRole,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
) {
    init {
        require(requestId.isNotBlank())
        require(familySpaceId.isNotBlank())
        require(deviceKeyId.isNotBlank())
        require(createdAtEpochMillis >= 0L)
        require(expiresAtEpochMillis > createdAtEpochMillis)
        require(receivedAtEpochMillis >= 0L)
    }
}
