# Device Identity and Initial Trust

## Status

This document defines requirements and a proposed trust model. The
`:core:identity` module contains a protocol-neutral public identity model,
a `DeviceKeyManager` interface, and typed expected failure categories. The
public-key byte array is copied on input and on access to prevent callers from
mutating the model's contents. The module does not implement key generation,
Android Keystore access, authentication, signatures, enrollment, or recovery.
FamilyMesh still has local profile IDs and local member metadata only.

## Separate identifiers

The implementation must keep these values distinct:

- **Profile ID** — random local database identifier used to find the local
  profile. It is not proof of identity and must never be used as a credential.
- **Device key pair** — a future cryptographic key pair used to prove
  possession of a device-specific private key.
- **Device identity** — a stable protocol-level identifier derived from or
  bound to a public key. Its exact encoding and rotation rules remain to be
  selected.
- **Member ID** — a family-scoped record identifier assigned during trusted
  enrollment. It is not necessarily equal to the profile ID or device ID.
- **Display name** — user-editable presentation data. It is never an identity
  or authorization factor.

## Identity module boundary

The current module exposes only public metadata: a key ID, opaque public-key
bytes, and a creation timestamp. It copies byte data both when constructed and
when read, and rejects blank IDs, empty public keys, and negative timestamps.
The byte encoding is intentionally unspecified until a protocol is selected.

`DeviceKeyManager` is the future provider boundary. Its contract returns
public identity metadata only; it has no method that exposes private key
material. It specifies that successful repeated reads should return the same
identity and that key invalidation must not trigger silent key replacement.
Typed failures distinguish unavailable secure storage, invalidated keys,
unsupported reviewed configuration, and corrupt stored public metadata.
No production implementation is registered or used by the app yet. This
abstraction is not proof that a key exists, is hardware-backed, or is trusted
by a family.

## Proposed trust model

1. A device creates a key pair locally using an Android-supported secure key
   storage mechanism selected after API-level and hardware-capability review.
2. The private key should be non-exportable where the platform supports it.
   The app must not log, serialize, back up, or transmit private key material.
3. The device publishes only the public identity material required by the
   reviewed protocol.
4. A family is not trusted merely because a local family-space row exists.
   Initial ownership must be established through an explicit trust ceremony.
5. A joining device generates its own key pair and submits an enrollment
   request bound to that key. An already trusted authority verifies the
   invitation and approves the exact public identity and requested role.
6. Membership records bind a family-scoped member ID to the approved device
   identity. Role changes and device removal require authenticated,
   authorized operations.
7. Key rotation, device replacement, and recovery are explicit lifecycle
   operations; they must not silently reuse a profile ID as a substitute key.

## Initial-owner establishment

The initial owner is the first trusted authority for a family. A future
implementation must define one reviewed bootstrap method, for example an
explicit local creation-and-enrollment ceremony with a clearly identified
device key. Merely pressing “Create family”, possessing the phone, or knowing
a family name is insufficient proof by itself.

The ceremony must show the user which device is becoming the initial owner,
make the trust decision explicit, and persist an auditable local event. The
protocol must define what happens if the ceremony is interrupted or repeated.

## Device loss and recovery

Before implementation, specify:
- whether another already trusted device can approve a replacement device;
- whether recovery codes or an external recovery authority exist;
- how users are warned that losing all trusted devices may mean losing access;
- how old device keys are revoked and how offline peers learn of revocation;
- whether family ownership can be transferred, and how the last owner is
  prevented from accidentally removing all administrative authority.

Do not promise account recovery until a tested recovery protocol exists.

## Key lifecycle requirements

- Generate keys with a reviewed platform API and an explicit algorithm/curve
  choice approved for the selected protocol.
- Keep private key operations inside the secure key boundary where possible.
- Define key identifiers, public-key serialization, signatures, rotation,
  revocation, and migration before writing production code.
- Handle devices without hardware-backed key protection without silently
  claiming equivalent security.
- Treat app reinstall, device restore, OS upgrade, and key invalidation as
  expected lifecycle events.
- Never silently generate a replacement key after invalidation or metadata
  corruption; require an explicit recovery or re-enrollment decision.
- Keep cryptographic implementation behind a narrow interface so it can be
  tested and reviewed independently from UI and persistence.
- Never invent a custom cryptographic protocol or implement primitives from
  scratch.

## Threat model

The design must account for:
- a person who knows a display name or family-space ID but is not a member;
- a copied, leaked, expired, or replayed invitation;
- a joining client that requests an elevated role;
- a malicious or compromised enrolled device;
- a lost or stolen unlocked device;
- local database modification on a rooted or compromised device;
- stale or offline peers that have not received a revocation;
- key invalidation, app reinstall, and loss of all trusted devices.

Android Keystore can reduce private-key extraction risk, but it does not by
itself authenticate family membership, protect a compromised running app, or
solve recovery and revocation.

## Decisions required before implementation

- Select the protocol and public-key identity format.
- Decide whether a family can have multiple trusted owner devices.
- Define the initial-owner ceremony and ownership-transfer rules.
- Define device replacement, revocation propagation, and recovery.
- Define the authorization checks required for invitation creation and role
  changes.
- Review the design against supported Android API levels and test devices.

Until these decisions are reviewed, the application must continue to label
local profile and member data as local-only metadata.
