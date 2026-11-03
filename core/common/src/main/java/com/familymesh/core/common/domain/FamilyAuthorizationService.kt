package com.familymesh.core.common.domain

/**
 * A request to authorize an operation for a specific device in one family.
 *
 * The caller supplies identifiers, never a role. The trusted resolver must
 * derive the role from verified membership state.
 */
data class FamilyAuthorizationRequest(
    val familySpaceId: String,
    val actorDeviceKeyId: String,
    val action: FamilyAction,
) {
    init {
        require(familySpaceId.isNotBlank()) { "Family-space ID must not be blank" }
        require(actorDeviceKeyId.isNotBlank()) { "Actor device-key ID must not be blank" }
    }
}

/**
 * Membership facts returned by the trusted membership resolver.
 *
 * This is a data-transfer result, not cryptographic proof by itself. Only a
 * resolver backed by verified identity and current trusted membership data
 * should construct and return it.
 */
data class ResolvedFamilyActor(
    val familySpaceId: String,
    val memberId: String,
    val deviceKeyId: String,
    val role: FamilyRole,
    val membershipVersion: Long,
) {
    init {
        require(familySpaceId.isNotBlank()) { "Family-space ID must not be blank" }
        require(memberId.isNotBlank()) { "Member ID must not be blank" }
        require(deviceKeyId.isNotBlank()) { "Device-key ID must not be blank" }
        require(membershipVersion >= 0) { "Membership version must not be negative" }
    }
}

/**
 * Trusted boundary responsible for verifying the device and resolving its
 * current membership in the requested family space.
 *
 * Implementations must not trust a role supplied by the caller or a joining
 * device. They must validate the device identity and authoritative membership
 * state before returning an actor.
 */
fun interface TrustedFamilyMembershipResolver {
    suspend fun resolveActor(
        familySpaceId: String,
        actorDeviceKeyId: String,
    ): ResolvedFamilyActor?
}

sealed interface FamilyAuthorizationResult {
    data class Authorized(
        val actor: ResolvedFamilyActor,
        val action: FamilyAction,
    ) : FamilyAuthorizationResult

    data class Denied(
        val actor: ResolvedFamilyActor,
        val action: FamilyAction,
    ) : FamilyAuthorizationResult

    data class ActorNotVerified(
        val request: FamilyAuthorizationRequest,
    ) : FamilyAuthorizationResult
}

/**
 * Resolves membership first, then applies the role policy.
 *
 * This service does not implement identity verification or persistence.
 * Security depends on a trustworthy resolver and on callers enforcing this
 * result at every state-changing boundary.
 */
class FamilyAuthorizationService(
    private val membershipResolver: TrustedFamilyMembershipResolver,
) {
    suspend fun authorize(
        request: FamilyAuthorizationRequest,
    ): FamilyAuthorizationResult {
        val actor = membershipResolver.resolveActor(
            familySpaceId = request.familySpaceId,
            actorDeviceKeyId = request.actorDeviceKeyId,
        ) ?: return FamilyAuthorizationResult.ActorNotVerified(request)

        if (
            actor.familySpaceId != request.familySpaceId ||
            actor.deviceKeyId != request.actorDeviceKeyId
        ) {
            return FamilyAuthorizationResult.ActorNotVerified(request)
        }

        return if (FamilyPermissionPolicy.canPerform(actor.role, request.action)) {
            FamilyAuthorizationResult.Authorized(actor, request.action)
        } else {
            FamilyAuthorizationResult.Denied(actor, request.action)
        }
    }
}
