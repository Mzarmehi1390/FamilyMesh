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

## Open design decisions

- Is there a trusted initial owner, and how is that authority established?
- Is enrollment peer-to-peer, relay-assisted, or both?
- What cryptographic protocol and key lifecycle will bind a device to a member?
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
