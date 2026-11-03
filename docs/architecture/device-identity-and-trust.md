# Device Identity and Initial Trust

## Status

This document defines requirements and a proposed trust model. The
`:core:identity` module contains a protocol-neutral public identity model,
a `DeviceKeyManager` interface, and typed expected failure categories. The
public-key byte array is copied on input and on access to prevent callers from
mutating the model's contents. The module includes an Android Keystore-backed P-256 signing-key provider. It
can create and read public identity metadata and sign caller-supplied bytes
without exporting the private key. This does not implement enrollment,
message encryption, protocol serialization, or account recovery.
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

`DeviceKeyManager` separates reading an existing identity from explicitly
creating a new one. A missing identity is not permission to replace an old
identity; a provider must distinguish a genuinely unprovisioned installation
from inconsistent or missing key material and report a typed failure.
Creating an identity must not overwrite one that already exists. The API
returns public metadata only and never exposes private key material.

Typed failures distinguish unavailable secure storage, invalidated keys,
missing key material, an already provisioned identity, unsupported reviewed
configuration, and corrupt stored public metadata. `AndroidKeystoreDeviceKeyManager` is registered in app dependency injection.
It uses a non-exportable Android Keystore EC signing key, stores only a
public-key fingerprint marker in private app preferences, and refuses to
silently replace missing or inconsistent key material. Its security-level
query reports what Android Keystore says for the generated key; software
backing remains possible. This local key is not proof of family membership
or remote trust.

## Local identity lifecycle policy

The identity module includes a pure policy that reconciles stored public metadata
with the observed platform-key state. Matching metadata and a present key are
`READY`; both artifacts absent are `NOT_PROVISIONED`; any mismatch, corrupt
metadata, or invalidated key is `RECOVERY_REQUIRED`; an unavailable Keystore is
`TEMPORARILY_UNAVAILABLE`. The policy does not create or repair keys.

`NOT_PROVISIONED` describes only the artifacts visible to the current
installation. It cannot prove that the app has never belonged to a family:
a reinstall may remove both local metadata and the app's Keystore key. A fresh
installation therefore still needs a trusted enrollment decision before it can
join an existing family.

## Reinstall and restore considerations

Android Keystore key material is tied to the app/device security environment;
it must not be assumed to survive uninstall, device replacement, or restore.
The app must not back up private key material. If public metadata and the
Keystore alias disagree, the provider must fail closed and require an explicit
recovery or re-enrollment decision. A fresh install cannot independently
prove that it is the same family member as a previous installation. A trusted
family authority or a separately reviewed recovery mechanism must approve a
replacement identity. The app must not claim that reinstall preserves identity.

## Implemented local key provider

The Android implementation currently generates an EC key on `secp256r1` with
`SHA256withECDSA` signing authorization, stores the private key under a
stable app-specific Android Keystore alias, and exposes only the X.509 Subject
Public Key Info bytes. The key ID is a versioned SHA-256 fingerprint over
those encoded public-key bytes. The creation timestamp comes from the
Keystore-generated certificate validity start. A small private-preferences
marker detects a missing key after prior provisioning; the private key itself
is never stored in preferences or backup data.

The provider checks that the public certificate, key marker, and private key
agree. It maps key invalidation and missing/corrupt state to typed failures.
Signing is available through a narrow `DeviceMessageSigner` interface and
returns the JCA ECDSA signature encoding. A signed enrollment-request envelope
now binds request ID, family-space ID, device-key ID, requested role, creation
and expiry times, protocol version, and a fresh 32-byte nonce using a fixed
order, length-prefixed v1 payload. Verification checks the public-key
fingerprint, signature, and validity window. This envelope is not COSE, does
not prove the requested role was authorized, and has no persistent replay
cache; an approving family authority and one-time nonce store are still
required. Hardware security level
is reported as an observation and is not assumed. This code has been built
and unit-tested in CI, but device-specific Keystore behavior still requires
Android API 31+ instrumentation testing on physical/emulated devices.

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


## Implemented local initial-owner bootstrap

The app offers an explicit confirmation to bind a locally saved family space
to the current Android Keystore device-key ID as its initial owner. Room
persists the initial-owner record and corresponding OWNER member row in one
transaction. A family-space primary key on the trust record prevents a second
local bootstrap; a failed owner-member insert rolls back the trust row. The
UI displays the stored owner key ID and does not silently assign ownership
when a family space is created.

This is only a local trust anchor on the current installation. It is not a
remote attestation, does not synchronize to other devices, and does not yet
provide signed owner approvals or recovery. A person with database write
access could alter local state; the table alone is not a cross-device
authorization proof.


## Transferable initial-owner trust bundle

The identity module can now export a bounded `FMOT1.` text bundle containing
the family-space ID, owner-member ID, owner device-key ID, establishment
timestamp, public key, and a signature over a domain-separated, versioned
payload. The bundle is generated only when the current Keystore identity
matches the local initial-owner trust record. Its decoder validates field
bounds, exact payload consumption, key fingerprint, and signature.

A valid bundle is **self-signed**, not self-authenticating: it proves that the
included key signed the statement, but does not prove that the key belongs to
the intended family owner. Before pinning it, another device must compare the
full key fingerprint with the owner through an independent trusted channel
(for example, an in-person comparison). The bundle is not encrypted, does not
provide confidentiality, does not synchronize trust automatically, and does
not yet implement remote pin storage or signed membership decisions.
