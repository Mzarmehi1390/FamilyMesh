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


/**
 * A domain-level authorization outcome. The supplied role is only an input
 * to this pure policy; callers must not treat an untrusted/local role as proof.
 */
sealed interface FamilyAuthorizationDecision {
    data class Allowed(
        val actorRole: FamilyRole,
        val action: FamilyAction,
    ) : FamilyAuthorizationDecision

    data class Denied(
        val actorRole: FamilyRole,
        val action: FamilyAction,
    ) : FamilyAuthorizationDecision
}

/**
 * Evaluates a proposed action without performing it or accessing persistence.
 *
 * This helper is suitable for use-case composition and tests, but is not a
 * security boundary until actorRole comes from verified membership state and
 * every state-changing operation checks it at its trusted boundary.
 */
object FamilyActionAuthorizer {
    fun evaluate(
        actorRole: FamilyRole,
        action: FamilyAction,
    ): FamilyAuthorizationDecision =
        if (FamilyPermissionPolicy.canPerform(actorRole, action)) {
            FamilyAuthorizationDecision.Allowed(actorRole, action)
        } else {
            FamilyAuthorizationDecision.Denied(actorRole, action)
        }

    fun requireAllowed(actorRole: FamilyRole, action: FamilyAction) {
        when (val decision = evaluate(actorRole, action)) {
            is FamilyAuthorizationDecision.Allowed -> Unit
            is FamilyAuthorizationDecision.Denied ->
                throw FamilyActionNotPermittedException(
                    actorRole = decision.actorRole,
                    action = decision.action,
                )
        }
    }
}

/** Typed denial for a role-policy check; it does not imply actor authentication. */
class FamilyActionNotPermittedException(
    val actorRole: FamilyRole,
    val action: FamilyAction,
) : IllegalStateException("Role $actorRole is not permitted to perform $action")
