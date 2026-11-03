package com.familymesh.core.common.domain

/**
 * Lifecycle of a proposed device enrollment.
 *
 * This is a local domain model only. It does not authenticate the requester,
 * inviter, or approving authority, and it does not grant family membership.
 */
enum class EnrollmentRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REVOKED,
    EXPIRED,
}

/**
 * A request to bind one device-key identifier to one family space.
 *
 * [requestedRole] is the role proposed by the trusted inviter/authority; it
 * must never be accepted from an untrusted joining client as an authority.
 * The current app has no authenticated authority implementation.
 */
data class DeviceEnrollmentRequest(
    val id: String,
    val familySpaceId: String,
    val deviceKeyId: String,
    val requestedRole: FamilyRole,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val status: EnrollmentRequestStatus = EnrollmentRequestStatus.PENDING,
) {
    init {
        require(id.isNotBlank()) { "Enrollment request ID must not be blank." }
        require(familySpaceId.isNotBlank()) { "Family space ID must not be blank." }
        require(deviceKeyId.isNotBlank()) { "Device key ID must not be blank." }
        require(createdAtEpochMillis >= 0L) { "Creation time must not be negative." }
        require(expiresAtEpochMillis > createdAtEpochMillis) {
            "Expiry time must be later than creation time."
        }
    }

    fun approve(atEpochMillis: Long): DeviceEnrollmentRequest =
        transition(atEpochMillis, EnrollmentRequestStatus.APPROVED, allowAtExpiry = false)

    fun reject(atEpochMillis: Long): DeviceEnrollmentRequest =
        transition(atEpochMillis, EnrollmentRequestStatus.REJECTED, allowAtExpiry = false)

    fun revoke(atEpochMillis: Long): DeviceEnrollmentRequest =
        transition(atEpochMillis, EnrollmentRequestStatus.REVOKED, allowAtExpiry = false)

    fun expire(atEpochMillis: Long): DeviceEnrollmentRequest {
        require(atEpochMillis >= expiresAtEpochMillis) {
            "A request can only expire at or after its expiry time."
        }
        return transition(atEpochMillis, EnrollmentRequestStatus.EXPIRED, allowAtExpiry = true)
    }

    private fun transition(
        atEpochMillis: Long,
        nextStatus: EnrollmentRequestStatus,
        allowAtExpiry: Boolean,
    ): DeviceEnrollmentRequest {
        require(atEpochMillis >= createdAtEpochMillis) {
            "Transition time must not be earlier than creation time."
        }
        require(status == EnrollmentRequestStatus.PENDING) {
            "Only pending requests can change state."
        }
        if (!allowAtExpiry) {
            require(atEpochMillis < expiresAtEpochMillis) {
                "An expired request cannot be approved, rejected, or revoked."
            }
        }
        return copy(status = nextStatus)
    }
}
