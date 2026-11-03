package com.familymesh.core.common.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyPermissionPolicyTest {
    @Test
    fun owner_canPerformEveryActionExceptRemovingOwner() {
        FamilyAction.values().forEach { action ->
            assertEquals(
                action != FamilyAction.REMOVE_OWNER,
                FamilyPermissionPolicy.canPerform(FamilyRole.OWNER, action),
            )
        }
    }

    @Test
    fun admin_canManageInvitationsAndRegularMembersOnly() {
        val allowed = setOf(
            FamilyAction.CREATE_INVITATION,
            FamilyAction.APPROVE_MEMBER_ENROLLMENT,
            FamilyAction.ASSIGN_MEMBER_ROLE,
            FamilyAction.REVOKE_MEMBER,
        )

        FamilyAction.values().forEach { action ->
            assertEquals(
                action in allowed,
                FamilyPermissionPolicy.canPerform(FamilyRole.ADMIN, action),
            )
        }
    }

    @Test
    fun member_cannotPerformAdministrativeActions() {
        FamilyAction.values().forEach { action ->
            assertEquals(
                false,
                FamilyPermissionPolicy.canPerform(FamilyRole.MEMBER, action),
            )
        }
    }

    @Test
    fun adminCannotChangeAdministrativeAuthority() {
        assertEquals(
            false,
            FamilyPermissionPolicy.canPerform(FamilyRole.ADMIN, FamilyAction.ASSIGN_ADMIN_ROLE),
        )
        assertEquals(
            false,
            FamilyPermissionPolicy.canPerform(FamilyRole.ADMIN, FamilyAction.REVOKE_ADMIN),
        )
        assertEquals(
            false,
            FamilyPermissionPolicy.canPerform(FamilyRole.ADMIN, FamilyAction.TRANSFER_OWNERSHIP),
        )
        assertEquals(
            false,
            FamilyPermissionPolicy.canPerform(FamilyRole.ADMIN, FamilyAction.REMOVE_OWNER),
        )
    }

    @Test
    fun ownerCannotBeRemovedByAnyRole() {
        FamilyRole.values().forEach { role ->
            assertEquals(
                false,
                FamilyPermissionPolicy.canPerform(role, FamilyAction.REMOVE_OWNER),
            )
        }
    }
}


class FamilyActionAuthorizerTest {
    @Test
    fun evaluate_returnsAllowedForPermittedAction() {
        assertEquals(
            FamilyAuthorizationDecision.Allowed(
                FamilyRole.ADMIN,
                FamilyAction.CREATE_INVITATION,
            ),
            FamilyActionAuthorizer.evaluate(
                FamilyRole.ADMIN,
                FamilyAction.CREATE_INVITATION,
            ),
        )
    }

    @Test
    fun evaluate_returnsDeniedForForbiddenAction() {
        assertEquals(
            FamilyAuthorizationDecision.Denied(
                FamilyRole.ADMIN,
                FamilyAction.TRANSFER_OWNERSHIP,
            ),
            FamilyActionAuthorizer.evaluate(
                FamilyRole.ADMIN,
                FamilyAction.TRANSFER_OWNERSHIP,
            ),
        )
    }

    @Test
    fun requireAllowed_returnsNormallyForPermittedAction() {
        FamilyActionAuthorizer.requireAllowed(
            FamilyRole.OWNER,
            FamilyAction.TRANSFER_OWNERSHIP,
        )
    }

    @Test
    fun requireAllowed_throwsTypedExceptionForForbiddenAction() {
        try {
            FamilyActionAuthorizer.requireAllowed(
                FamilyRole.MEMBER,
                FamilyAction.CREATE_INVITATION,
            )
            throw AssertionError("Expected FamilyActionNotPermittedException")
        } catch (error: FamilyActionNotPermittedException) {
            assertEquals(FamilyRole.MEMBER, error.actorRole)
            assertEquals(FamilyAction.CREATE_INVITATION, error.action)
        }
    }
}
