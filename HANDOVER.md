# FamilyMesh Handover

Last synchronized: 2026-11-03
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Last verified repository state

- Branch: `main`
- Last verified source commit: `21dd7ca4db718ee31416e45319d88ffce9c950c3`
- Commit subject: `fix(identity): resolve owner pin inside decision acceptance`
- Commit timestamp: 2026-11-03T15:48:14Z
- Latest source CI run: [Android CI #41671995919](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41671995919)
- CI result: unit tests passed, debug APK assembled, artifact uploaded.
- Branch protection: disabled; no open issues or pull requests were present at synchronization.

## Product and architecture invariants

- Native Android only: Kotlin, Jetpack Compose, Gradle Kotlin DSL.
- Preserve modular architecture: `app`, `core:common`, `core:storage`,
  `core:designsystem`, `core:identity`.
- FamilyMesh remains private, decentralized, and end-to-end encrypted by
  design. Do not replace P2P with a centralized messaging backend.
- Never claim E2EE, network P2P, BLE, Wi-Fi Direct, relay, or production
  membership authorization exists; those are not implemented.
- Keep all authorization fail-closed until a reviewed trusted membership
  resolver exists.
- Do not silently replace a lost/invalidated device key.

## Completed implementation visible at the last verified source commit

- Native Android foundation, Compose UI, Koin dependency injection.
- Room family-space and local member metadata persistence; Preferences
  DataStore.
- Android Keystore P-256 ECDSA signing identity and typed lifecycle policy.
- Signed device-enrollment request with nonce and expiry.
- Bounded manual enrollment proof text codec and local nonce replay storage.
- Local initial-owner bootstrap and signed trust bundle.
- Recipient-side owner fingerprint confirmation and immutable local pin.
- Signed owner decision (`FMOD1.`) verified against the pinned owner and
  exact pending request; persisted atomically.
- Applicant-signed acceptance (`FMJA1.`) bound to the exact approved decision.
- Owner-side verification of both signatures and local pending request.
- Room v7 authenticated-device record insertion and pending-request consumption
  in one transaction.
- Home screen lists authenticated devices for the selected family.
- Decision acceptance service resolves the owner key from `InitialOwnerPinRepository` internally; UI callers cannot inject an arbitrary owner trust record.
- All four enrollment transfer codecs reject non-canonical/padded Base64URL aliases.
- All enrollment signature verification boundaries reject non-P-256 EC public keys.
- Added tests rejecting missing, mismatched, and ambiguous owner pins.
- Unit tests cover domain, identity, and local persistence behavior.

## Latest code changes and verification

- Commits: `918b03b` (canonical Base64URL), `36e2d27` (P-256 enforcement), `21dd7ca` (parser cleanup).
- CI run #41671671596 passed unit tests, APK assembly, and artifact upload for canonical Base64URL changes.
- CI run #41671944679 passed unit tests, APK assembly, and artifact upload for P-256 enforcement.
- CI run #41671995919 passed unit tests, APK assembly, and artifact upload for the current source commit.

## Current work package

**WP — Identity and trusted enrollment protocol review.**

The local enrollment prototype is implemented, but its custom binary payloads
are not the proposed interoperable COSE protocol. The exact next action is to
complete a security review of canonical encoding, key identifiers, signature
representation, replay semantics, and enrollment trust/lifecycle before
connecting these records to authorization or implementing transport.

## Open blockers and risks

1. ADR 0001 is still proposed: code uses JCA P-256 / SHA256withECDSA and
   X.509 public-key bytes with custom domain-separated length-prefixed
   payloads; COSE ESP256 serialization/conversion is not implemented.
2. Nonce consumption is local to one Room database; it is not distributed
   replay protection.
3. Initial-owner trust pinning is a manual out-of-band fingerprint ceremony;
   ownership transfer and multi-owner semantics are undefined.
4. No production `TrustedFamilyMembershipResolver` exists;
   `DenyAllFamilyMembershipResolver` remains the default.
5. No device revocation, recovery, cross-device synchronization, network
   transport, BLE, Wi-Fi Direct, internet P2P, relay, or E2EE messaging.
6. Keystore behavior has not been verified by Android API 31+ instrumentation
   tests on physical/emulated devices.
7. Room migrations and transaction behavior need Android instrumentation
   coverage.
8. Gradle Wrapper is absent. CI currently installs Gradle 8.13 and JDK 17.
9. Original Master Engineering Prompt was not present among the accessible
   repository/project files during this synchronization. Do not invent its
   contents; use the repository roadmap and established project invariants.

## Next exact engineering action

Complete the signed enrollment chain
(`FMER1.` request → `FMOT1.` owner trust → `FMOD1.` decision →
`FMJA1.` applicant acceptance). Produce a reviewed decision record covering
canonical wire encoding, public-key/key-ID format, ECDSA signature encoding,
nonce/replay scope, clock assumptions, and cross-device test vectors. Keep
networking and authorization integration blocked until those decisions are
resolved and documented.
