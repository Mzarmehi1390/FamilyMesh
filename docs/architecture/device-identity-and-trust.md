# Device Identity and Initial Trust

## Status

This document records both implemented local identity/enrollment primitives
and the remaining proposed trust model. The `:core:identity` module contains
a public identity model, a `DeviceKeyManager` boundary, typed lifecycle
failures, an Android Keystore-backed P-256 signing-key provider, signed
enrollment requests, owner trust bundles and pins, signed owner decisions,
applicant-signed acceptance, and owner-side finalization services.

Public-key byte arrays are copied on input and access. The current key
representation exposed by Android Keystore is X.509 Subject Public Key Info;
the stable interoperable protocol representation and COSE encoding remain
under review. These primitives implement a local manually transferred
prototype only. They do not provide end-to-end message encryption, network
transport, distributed membership, revocation, or account recovery.

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
(for example, an in-person comparison). The bundle is not encrypted and does
not provide confidentiality or automatic trust synchronization. Recipient-side
pinning and signed owner decisions are implemented locally, but are not
distributed to other devices.


## Recipient-side initial-owner pinning

A receiving device can decode an `FMOT1.` bundle and validate its self-signature,
key fingerprint, and bounded encoding. The app displays the complete
fingerprint and requires the user to enter the value independently compared
with the owner over a separate trusted channel. Only after this explicit
confirmation is the owner public key pinned locally.

Pins are immutable per family-space ID in this preview: attempting to pin a
different owner key for the same family returns a conflict and does not
replace the existing record. Room schema version 5 persists the public key,
owner identifiers, establishment time, and local pin time. This is a
device-local trust decision; there is no automatic network distribution,
owner rotation, recovery, or membership approval yet. The UI cannot guarantee
that an out-of-band comparison was performed honestly; it makes the required
comparison explicit and blocks accidental trust based only on a valid
self-signature.


## Signed owner enrollment decisions

The identity module now defines a versioned, domain-separated owner decision
payload and a bounded `FMOD1.` text envelope. A decision binds the family-space
ID, exact request ID, applicant device-key ID, owner device-key ID, outcome,
and decision timestamp. The local signing service refuses to sign unless the
current device identity matches the locally established initial-owner key and
the request is inside its declared validity window.

Recipients verify the signature against the immutable owner public key they
previously pinned, and the verifier checks every request/owner binding plus
the request validity window and decision timestamp. The codec rejects
oversized fields, unsupported versions, malformed UTF-8, and trailing bytes.
Unit tests cover round-trip verification, request/family mismatch, a different
owner key, timestamp bounds, outcome tampering, and trailing data.

This artifact is a signed statement, **not a membership grant**. A recipient
can now validate an imported decision against the exact locally pending request
and the previously pinned owner key. After successful verification, Room
stores the complete signed decision and removes the matching pending request
in one transaction. A primary key on (familySpaceId, requestId) prevents
multiple conflicting outcomes for the same request; an exact repeated import
is idempotent, while a different decision conflicts. Schema version 6 adds
the decision table and a 5-to-6 migration.

The acceptance service loads the pending request from local storage rather
than trusting caller-supplied request fields. It checks the signature, owner
pin, family/request/applicant bindings, and decision time before asking the
repository to commit. The database transaction rechecks the pending row and
applicant key, then inserts the decision and deletes the pending row atomically.
Storage errors fail closed and coroutine cancellation is propagated.

No member row is created, no role is assigned, and no authorization state is
changed by an APPROVED decision. The persisted record is an auditable local
copy of the owner's signed statement, not proof that this device has joined
the family. There is still no network synchronization or cross-device replay
coordination.


## Applicant acceptance and owner-side local finalization

The current prototype now supports an applicant-signed acceptance receipt
for an exact approved `FMOD1.` owner decision. The `FMJA1.` envelope binds
the signed decision, applicant device-key ID, applicant public key, and
acceptance timestamp. The owner-side finalization service loads the pending
request from local storage, verifies the owner decision against the locally
established initial-owner identity, verifies that the applicant public-key
fingerprint matches the pending request's device-key ID, and verifies the
applicant signature and validity bounds.

After successful verification, Room schema version 7 persists an
`authenticated_family_devices` record and consumes the matching pending
request in one transaction. The operation checks request/device/role/time
bindings and treats an exact repeat as idempotent while conflicting records
are rejected. The home screen lists authenticated devices for the selected
family. Unit tests cover the domain services and repository behavior.

These records exist only on the owner installation. They are not synchronized
to the applicant or other peers, do not yet feed a production
`TrustedFamilyMembershipResolver`, and do not establish globally current
membership. There is no device revocation, ownership transfer, or recovery.

## Protocol status and review gates

The implemented local payloads use domain-separated, fixed-order,
length-prefixed binary encodings and bounded text envelopes (`FMOT1.`,
`FMER1.`, `FMOD1.`, and `FMJA1.`). The Android Keystore provider signs
with JCA `SHA256withECDSA` and exposes X.509 Subject Public Key Info public
keys. This is **not** a canonical COSE implementation. ADR 0001's P-256 /
COSE ESP256 profile remains proposed and must not be described as completed.

Before this prototype can become a production protocol, review:
- canonical encoding, strict parser behavior, and cross-language test vectors;
- versioned key-ID derivation and public-key serialization;
- JCA-to-COSE signature conversion and signature canonicality;
- persistent replay semantics across independent devices and clock skew;
- initial-owner ceremony, ownership transfer, revocation, and recovery;
- database tampering, compromised devices, and offline authorization;
- Android API 31+ Keystore behavior and Room migration/instrumentation tests.

Do not wire the local authenticated-device table into authorization or add
network transport until the membership authority and consistency model are
reviewed. The authorization service must remain fail-closed meanwhile.


## Protocol review progress — 2026-11-03

A source audit of `FMER1.`, `FMOT1.`, `FMOD1.`, and `FMJA1.` confirmed
that the current payloads use fixed field order, explicit version integers,
length-prefixed fields, domain-separated signing bytes, bounded parsers,
strict UTF-8 decoding, and trailing-data rejection. These are custom v1
formats; they are not COSE and are not yet approved as an interoperable
production wire protocol.

Two parser/algorithm-boundary hardenings have now been implemented:

- All four transfer codecs accept only the canonical unpadded Base64URL
  representation emitted by their encoders. Padding and alternate textual
  aliases are rejected by a shared decoder.
- All four signature-verification paths now parse only P-256 EC public keys.
  P-384 and other EC curves are rejected rather than silently broadening the
  proposed P-256/SHA-256 profile.

The key ID currently remains
`SHA-256("FamilyMesh:device-key:v1\\0" || X.509 SubjectPublicKeyInfo)`.
JCA `SHA256withECDSA` signatures remain in provider DER encoding. A COSE
implementation will require a separately specified canonical COSE_Key
representation, stable key-ID derivation over that representation, strict
DER-to-COSE fixed-width R||S conversion, and a documented low-S/canonical
signature policy.

Replay protection remains a local Room transaction: a 32-byte random nonce
is domain-hashed and stored under a unique local primary key alongside the
pending request; expired rows are pruned when another proof is consumed.
This does not prevent replay across separate installations or establish
distributed membership freshness. Timestamp checks currently use strict
millisecond epoch comparisons without a specified clock-skew tolerance or
maximum request lifetime.

The owner bundle remains self-signed and requires an independent fingerprint
comparison before local pinning. The pin is immutable per family on that
installation, not a globally synchronized authority record. Local database
tampering, owner transfer, revocation, recovery, and offline-peer freshness
remain unresolved.

Do not connect these local records to production authorization or transport
until the wire profile, trust ceremony, replay scope, lifecycle, and threat
model are approved.
