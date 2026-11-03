package com.familymesh.core.common.domain

/**
 * Operations that affect family membership or administrative authority.
 */
enum class FamilyAction {
    CREATE_INVITATION,
    APPROVE_MEMBER_ENROLLMENT,
    ASSIGN_MEMBER_ROLE,
    REVOKE_MEMBER,
    ASSIGN_ADMIN_ROLE,
    REVOKE_ADMIN,
    TRANSFER_OWNERSHIP,
    REMOVE_OWNER,
}

/**
 * Conservative, protocol-neutral role policy for review and unit testing.
 *
 * This policy is not yet wired into repositories or UI and is not an
 * authorization boundary until every state-changing operation enforces it
 * against an authenticated actor and current trusted membership state.
 */
object FamilyPermissionPolicy {
    fun canPerform(actorRole: FamilyRole, action: FamilyAction): Boolean =
        when (actorRole) {
            FamilyRole.OWNER -> action != FamilyAction.REMOVE_OWNER
            FamilyRole.ADMIN -> action in ADMIN_ACTIONS
            FamilyRole.MEMBER -> false
        }

    private val ADMIN_ACTIONS = setOf(
        FamilyAction.CREATE_INVITATION,
        FamilyAction.APPROVE_MEMBER_ENROLLMENT,
        FamilyAction.ASSIGN_MEMBER_ROLE,
        FamilyAction.REVOKE_MEMBER,
    )
}
