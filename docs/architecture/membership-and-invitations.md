# Membership and Invitation Design

## Status and scope

This document describes both the design baseline and the current local
prototype. FamilyMesh now has an Android Keystore device-signing identity,
manual signed enrollment-proof transfer, local initial-owner trust bootstrap
and pinning, signed owner decisions, applicant-signed acceptance, and
owner-side local finalization into an authenticated-device record.

The workflow is still device-local and manually transferred. It has no network
transport, cross-device synchronization, production membership resolver,
invitation lifecycle, revocation, or recovery. These local records must not be
presented as globally current membership or production authorization.

The next implementation must not treat a display name, local UUID, or locally
stored role as proof of identity or permission.

## Terms

- **Family space**: a logical group with a stable identifier.
- **Member record**: descriptive metadata shown in the local member list.
- **Device identity**: the current prototype's P-256 signing public key and
  versioned fingerprint; canonical interoperable serialization remains under
  review.
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
be approved by a trusted authority. A durable Room-backed nonce store now provides atomic one-time consumption
on the local authority device, expiring old nonce records only after their
signed request expiry. This is local replay protection for a single database;
it is not distributed replay protection and does not authenticate the
approving authority.

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

The local member-list metadata API still creates only a `MEMBER` entry.
Separately, the enrollment prototype stores an authenticated-device record
after owner-side verification and atomic finalization. These records are not
yet consumed by a production authorization resolver. `OWNER` and `ADMIN`
remain domain labels governed by a proposed policy, not end-to-end enforced
permissions. Ownership transfer, multiple-owner rules, and last-owner
protection remain unresolved.

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


## Transferable enrollment proof envelope

A versioned `FMER1.` Base64URL text envelope can carry the signed request and
the claimed public identity between devices (for example, by copy/paste or a
future QR presentation). Decoding is bounded and rejects unsupported versions,
malformed fields, and trailing bytes. The receiving side must still recompute
the public-key fingerprint and verify the signature and expiry. The embedded
public key is a claim from the requester, not a trust anchor. The envelope is
not encrypted, is not a secure transport, and does not approve membership.


## Local pending-request inbox

The receiving device verifies the signed proof and, in one Room transaction,
consumes its nonce and persists a minimal pending-request record. A failed
request insert rolls back nonce consumption; a repeated nonce is rejected.
The home screen displays received requests as awaiting a trusted owner decision.
This inbox is local to one database and is not synchronized. The embedded
public key and signature are verified before the request is stored; the inbox
stores request metadata, not private key material.

## Signed approval, applicant acceptance, and local finalization

The current prototype adds two explicit steps after the owner decision:

1. The owner creates a bounded, versioned `FMOD1.` decision bound to the
   family-space ID, exact request ID, applicant device-key ID, owner device-key
   ID, outcome, and decision timestamp. The receiving device verifies it
   against its immutable locally pinned owner key and exact pending request.
   The verified statement is persisted and the matching pending inbox row is
   consumed atomically. An approval alone does not create membership.
2. For an approved decision, the applicant signs an acceptance receipt using
   its device key. The receipt binds the exact signed decision, applicant
   key ID, applicant public key, and acceptance timestamp. The owner device
   verifies both the owner decision and applicant acceptance against its
   locally stored pending request and initial-owner key.

After successful verification, the owner device atomically inserts an
`authenticated_family_devices` record and removes the pending request.
Room schema version 7 adds this table and a unique per-family device-key index.
The home screen exposes these records for the selected family.

This is a local owner-side record, not a distributed membership system. The
applicant's device does not receive a synchronized membership record through
this flow; there is no network protocol, membership-version propagation,
production authorization resolver, revocation, ownership transfer, or
recovery. The existing `FamilyAuthorizationService` remains fail-closed via
`DenyAllFamilyMembershipResolver` and is not wired to these records.

The current payload format is a custom fixed-order, length-prefixed v1
encoding with domain separation and bounded manual text envelopes. It is not
COSE serialization. The P-256 / COSE ESP256 ADR remains proposed; key-ID
canonicalization, COSE signature conversion, interoperability, persistent
replay semantics across devices, and end-to-end threat-model review remain
open gates.
