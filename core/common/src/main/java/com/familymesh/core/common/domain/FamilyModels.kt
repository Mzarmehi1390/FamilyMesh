package com.familymesh.core.common.domain

/**
 * Domain representation of a family space.
 *
 * IDs are assigned by a future enrollment/repository layer. This model does not
 * imply that a space exists remotely or that a device has authenticated.
 */
data class FamilySpace(
    val id: String,
    val name: String,
    val createdAtEpochMillis: Long
) {
    init {
        require(id.isNotBlank()) { "Family space ID must not be blank." }
        require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH) {
            "Family space name must contain 1..$MAX_NAME_LENGTH characters."
        }
        require(createdAtEpochMillis >= 0) { "Creation time must not be negative." }
    }

    companion object {
        const val MAX_NAME_LENGTH = 60
    }
}

enum class FamilyRole {
    OWNER,
    ADMIN,
    MEMBER
}

/** A member record within one family space; it is not an authentication credential. */
data class FamilyMember(
    val id: String,
    val familySpaceId: String,
    val displayName: String,
    val role: FamilyRole,
    val joinedAtEpochMillis: Long
) {
    init {
        require(id.isNotBlank()) { "Member ID must not be blank." }
        require(familySpaceId.isNotBlank()) { "Family space ID must not be blank." }
        require(displayName.isNotBlank() && displayName.length <= MAX_DISPLAY_NAME_LENGTH) {
            "Display name must contain 1..$MAX_DISPLAY_NAME_LENGTH characters."
        }
        require(joinedAtEpochMillis >= 0) { "Join time must not be negative." }
    }

    companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 40
    }
}
