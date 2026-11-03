package com.familymesh.core.common.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyAuthorizationServiceTest {
    private val actor = ResolvedFamilyActor(
        familySpaceId = "family-1",
        memberId = "member-1",
        deviceKeyId = "device-key-1",
        role = FamilyRole.ADMIN,
        membershipVersion = 3,
    )

    @Test
    fun authorize_usesResolvedMembershipRole_notCallerSuppliedRole() = runBlocking {
        var requestedFamily: String? = null
        var requestedDevice: String? = null
        val service = FamilyAuthorizationService(
            TrustedFamilyMembershipResolver { familyId, deviceKeyId ->
                requestedFamily = familyId
                requestedDevice = deviceKeyId
                actor
            },
        )

        val request = FamilyAuthorizationRequest(
            familySpaceId = "family-1",
            actorDeviceKeyId = "device-key-1",
            action = FamilyAction.CREATE_INVITATION,
        )

        assertEquals(
            FamilyAuthorizationResult.Authorized(actor, FamilyAction.CREATE_INVITATION),
            service.authorize(request),
        )
        assertEquals("family-1", requestedFamily)
        assertEquals("device-key-1", requestedDevice)
    }

    @Test
    fun authorize_returnsDenied_whenResolvedRoleLacksPermission() = runBlocking {
        val service = FamilyAuthorizationService(
            TrustedFamilyMembershipResolver { _, _ -> actor },
        )

        assertEquals(
            FamilyAuthorizationResult.Denied(actor, FamilyAction.TRANSFER_OWNERSHIP),
            service.authorize(
                FamilyAuthorizationRequest(
                    familySpaceId = "family-1",
                    actorDeviceKeyId = "device-key-1",
                    action = FamilyAction.TRANSFER_OWNERSHIP,
                ),
            ),
        )
    }

    @Test
    fun authorize_returnsActorNotVerified_whenResolverHasNoMembership() = runBlocking {
        val service = FamilyAuthorizationService(
            TrustedFamilyMembershipResolver { _, _ -> null },
        )
        val request = FamilyAuthorizationRequest(
            familySpaceId = "family-1",
            actorDeviceKeyId = "unknown-device",
            action = FamilyAction.CREATE_INVITATION,
        )

        assertEquals(FamilyAuthorizationResult.ActorNotVerified(request), service.authorize(request))
    }

    @Test
    fun authorize_rejectsResolverResultForDifferentDeviceOrFamily() = runBlocking {
        val mismatchedActor = actor.copy(deviceKeyId = "other-device")
        val service = FamilyAuthorizationService(
            TrustedFamilyMembershipResolver { _, _ -> mismatchedActor },
        )

        val request = FamilyAuthorizationRequest(
            familySpaceId = "family-1",
            actorDeviceKeyId = "device-key-1",
            action = FamilyAction.CREATE_INVITATION,
        )

        assertEquals(FamilyAuthorizationResult.ActorNotVerified(request), service.authorize(request))
    }

    @Test
    fun authorize_rejectsResolverResultForDifferentFamily() = runBlocking {
        val mismatchedActor = actor.copy(familySpaceId = "other-family")
        val service = FamilyAuthorizationService(
            TrustedFamilyMembershipResolver { _, _ -> mismatchedActor },
        )
        val request = FamilyAuthorizationRequest(
            familySpaceId = "family-1",
            actorDeviceKeyId = "device-key-1",
            action = FamilyAction.CREATE_INVITATION,
        )

        assertEquals(FamilyAuthorizationResult.ActorNotVerified(request), service.authorize(request))
    }

    @Test
    fun authorize_returnsResolverUnavailable_whenResolverThrows() = runBlocking {
        val service = FamilyAuthorizationService(
            TrustedFamilyMembershipResolver { _, _ -> error("membership store unavailable") },
        )
        val request = FamilyAuthorizationRequest(
            familySpaceId = "family-1",
            actorDeviceKeyId = "device-key-1",
            action = FamilyAction.CREATE_INVITATION,
        )

        assertEquals(FamilyAuthorizationResult.ResolverUnavailable(request), service.authorize(request))
    }

    @Test
    fun authorize_propagatesCoroutineCancellation() = runBlocking {
        val service = FamilyAuthorizationService(
            TrustedFamilyMembershipResolver { _, _ -> throw CancellationException("cancelled") },
        )

        try {
            service.authorize(
                FamilyAuthorizationRequest(
                    familySpaceId = "family-1",
                    actorDeviceKeyId = "device-key-1",
                    action = FamilyAction.CREATE_INVITATION,
                ),
            )
            throw AssertionError("Expected coroutine cancellation to propagate")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    @Test
    fun authorizationService_usesDenyAllResolverByDefault() = runBlocking {
        val service = FamilyAuthorizationService()
        val request = FamilyAuthorizationRequest(
            familySpaceId = "family-1",
            actorDeviceKeyId = "device-key-1",
            action = FamilyAction.CREATE_INVITATION,
        )

        assertEquals(FamilyAuthorizationResult.ActorNotVerified(request), service.authorize(request))
    }

    @Test
    fun authorizationRequest_rejectsBlankIdentifiers() {
        try {
            FamilyAuthorizationRequest("", "device-key-1", FamilyAction.CREATE_INVITATION)
            throw AssertionError("Expected invalid family-space ID to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }

        try {
            FamilyAuthorizationRequest("family-1", " ", FamilyAction.CREATE_INVITATION)
            throw AssertionError("Expected invalid device-key ID to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun resolvedActor_rejectsNegativeMembershipVersion() {
        try {
            actor.copy(membershipVersion = -1)
            throw AssertionError("Expected negative membership version to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}


class DenyAllFamilyMembershipResolverTest {
    @Test
    fun resolverNeverReturnsAnActor() = runBlocking {
        assertEquals(
            null,
            DenyAllFamilyMembershipResolver.resolveActor(
                familySpaceId = "family-1",
                actorDeviceKeyId = "device-key-1",
            ),
        )
    }

    @Test
    fun authorizationServiceFailsClosedWithDefaultResolver() = runBlocking {
        val service = FamilyAuthorizationService(DenyAllFamilyMembershipResolver)
        val request = FamilyAuthorizationRequest(
            familySpaceId = "family-1",
            actorDeviceKeyId = "device-key-1",
            action = FamilyAction.CREATE_INVITATION,
        )

        assertEquals(
            FamilyAuthorizationResult.ActorNotVerified(request),
            service.authorize(request),
        )
    }
}
