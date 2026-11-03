# Membership and Invitation Design

## Status and scope

This document is a design baseline, not an implementation claim. FamilyMesh
currently stores family spaces and member-list entries on one device. It has no
trusted identity, invitation protocol, remote enrollment, synchronization, or
authorization enforcement.

The next implementation must not treat a display name, local UUID, or locally
stored role as proof of identity or permission.

## Terms

- **Family space**: a logical group with a stable identifier.
- **Member record**: descriptive metadata shown in the local member list.
- **Device identity**: a future cryptographic identity bound to a device key.
- **Invitation**: a time-limited authorization offer to request enrollment in
  one specific family space.
- **Enrollment**: the verified transition that binds a device identity to a
  family space and records its membership.
- **Role**: an authorization attribute assigned by an authorized authority;
  it must never be accepted solely because a joining client supplied it.

## Proposed lifecycle

1. **Create** — A family space is created locally. Until a trusted creator
   identity exists, its local record is not considered an authoritative group.
2. **Prepare invitation** — An authorized member requests an invitation for a
   specific family space and a narrowly defined role. The invitation has an
   explicit expiry and usage limit.
3. **Share** — The invitation is transferred out-of-band by a user-chosen
   channel. The UI must explain that anyone possessing a bearer invitation
   may be able to attempt redemption, depending on the eventual protocol.
4. **Review** — The receiving user sees the family-space identity and the
   requested role, and explicitly confirms before enrollment.
5. **Verify and enroll** — A future protocol verifies the invitation and
   binds the joining device's public identity to the membership record.
   The server/peer authority, if any, must validate the inviter's authority.
6. **Revoke** — An authorized member can revoke an unused invitation. A
   separate membership-removal flow is required for already enrolled devices.
7. **Expire and consume** — Expired, revoked, or already-consumed invitations
   are rejected. Redemption must be atomic to prevent concurrent reuse.

## Local enrollment-request state model

The common domain module now defines a protocol-neutral enrollment request
bound to one family-space ID, one device-key ID, a proposed role, and an
explicit expiry. Its local lifecycle permits only a pending request to become
approved, rejected, revoked, or expired. Approval, rejection, and revocation
are disallowed at or after expiry; expiration is allowed only at or after the
expiry timestamp. Terminal states cannot transition again.

A signed request envelope is now available in `:core:identity`. It signs the
request ID, family ID, device-key ID, requested role, creation/expiry times,
protocol version, and a random nonce with the device's P-256 key. A verifier
can validate the signature against the supplied public identity and check the
validity window. This proves possession of the joining device's key only; it
does not authenticate the inviter, authorize the requested role, grant
membership, or prevent replay across devices. The requested role must still
be approved by a trusted authority. Persistent one-time nonce tracking,
authenticated authority, and distributed replay protection remain
unimplemented.

## Security requirements before implementation

- Bind every invitation to exactly one family-space identifier.
- Enforce expiry and a single-use or explicitly bounded-use policy.
- Prevent replay and concurrent double redemption.
- Do not allow the invitee to choose or elevate their own role.
- Verify the inviter's authority at redemption time, not only when an invite
  was displayed or created.
- Require explicit user confirmation of the family space and requested role.
- Avoid placing long-lived secrets in logs, analytics, clipboard history, or
  crash reports. The UI should warn before copying or sharing sensitive invite
  material.
- Define revocation behavior for both outstanding invitations and existing
  member devices.
- Define how offline devices learn about revocations and how conflicts are
  resolved before claiming revocation is immediate everywhere.
- Protect against duplicate identities, stale membership state, clock skew,
  malicious peers, and interrupted enrollment.
- Keep display names separate from identity and authorization decisions.

## Role policy

The current local API can only create a `MEMBER` metadata entry. This is
intentional. `OWNER` and `ADMIN` are domain labels today, not enforced
permissions. A future role policy must define who can grant each role, whether
there can be multiple owners, and how ownership transfer and last-owner
removal work. No UI should imply that these permissions are enforced before
that policy and its enforcement are implemented.

## Initial role-permission policy (proposed)

The common domain module now contains a pure policy for review and testing.
The proposed baseline is deliberately conservative:

| Action | OWNER | ADMIN | MEMBER |
|---|---:|---:|---:|
| Create invitation | Yes | Yes | No |
| Approve regular-member enrollment | Yes | Yes | No |
| Assign or revoke MEMBER role | Yes | Yes | No |
| Assign or revoke ADMIN role | Yes | No | No |
| Transfer ownership | Yes | No | No |
| Remove an OWNER | No | No | No |

Ownership changes must use the explicit transfer operation; an owner cannot
be removed through the ordinary member-removal operation. ADMIN cannot grant,
remove, or transfer administrative authority. This is a proposed product
policy, not a claim that the app currently enforces permissions. The policy
must be checked against an authenticated actor and current trusted membership
at every state-changing boundary before it can provide security.

## Authorization service boundary

`FamilyAuthorizationService` accepts a family-space ID, device-key ID, and
action; it deliberately does not accept a caller-selected role. Its injected
`TrustedFamilyMembershipResolver` must verify the device identity and resolve
its current membership and role for that exact family. The service rejects a
missing membership or a resolver result whose family/device identifiers do
not match the request, then applies the proposed role policy. Resolver
failures produce an explicit `ResolverUnavailable` result and never grant
access; coroutine cancellation is propagated rather than swallowed. The
service defaults to `DenyAllFamilyMembershipResolver` when no resolver is
provided.

No production resolver exists yet, and the current local Room member records
are not trusted membership evidence. A `DenyAllFamilyMembershipResolver` is
provided as a fail-closed default and is also the constructor default: it
always reports no verified actor, so the authorization service cannot grant
access until a reviewed resolver is implemented. Resolver exceptions are
reported as unavailable, not authorized. The service is not wired to membership mutations, invitation
operations, or UI. The next implementation must supply a resolver only after
identity verification, membership authority, and staleness/version semantics
are specified.

## Open design decisions

- Is there a trusted initial owner, and how is that authority established?
- Is enrollment peer-to-peer, relay-assisted, or both?
- How will the proposed P-256/COSE device-signature profile be integrated with membership, replay protection, and key lifecycle?
- Are invitations bearer tokens, public-key-bound requests, or a two-step
  approval exchange?
- What is the recovery path when every enrolled device is lost?
- What consistency guarantees apply while devices are offline?

These questions require a dedicated identity and threat-model review. Do not
implement networking or invitation secrets until the decisions are recorded
and reviewed.

## Acceptance criteria

The design is ready to move toward implementation only when:
- identity and initial-owner establishment are specified;
- invitation creation, confirmation, redemption, expiry, and revocation are
  state-defined;
- role assignment and enforcement are specified;
- replay, reuse, stale state, offline operation, and device-loss cases have
  documented expected outcomes;
- the selected cryptographic building blocks and key lifecycle have been
  reviewed.
