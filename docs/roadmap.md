# Roadmap

## Milestone 0 — Project Foundation

- [x] Android project skeleton and module boundaries
- [x] Kotlin and Jetpack Compose setup
- [x] Android 12 minimum platform policy (API 31)
- [x] Dependency injection bootstrap
- [x] Preferences DataStore foundation
- [x] Room runtime and compiler foundation
- [x] Basic launch screen and shared theme
- [x] Unit-test baseline
- [x] GitHub Actions workflow
- [x] Verify unit tests, lint, debug APK, Room schema, and Android instrumentation on GitHub Actions Ubuntu/JDK 17/Gradle 8.13 (PR #2 run #45885409892 passed all three jobs on exact head 09eaa3832ac50e13b27506953d310adfae4b2f46)
- [x] Add and validate the official Gradle 8.13 Wrapper (JAR and distribution checksums pinned)
- [x] Add Android 12+ cloud-backup and device-transfer exclusions with a fail-closed verifier and 15 regression tests
- [x] Add an adaptive launcher icon and remove its redundant API-26 resource qualifier
- [x] Update Room to 2.8.5 (no schema change)
- [x] Reject P-256 point-at-infinity imports in JVM and Android-provider tests

## Milestone 1 — Domain and identity

- [x] Define immutable family-space and member domain models
- [x] Define repository contract for observing family spaces and members
- [x] Implement device-local Room persistence for family spaces
- [x] Add local member-list persistence (metadata only; not authenticated enrollment)
- [x] Document proposed invitation and membership lifecycle
- [x] Document device identity, initial trust, key lifecycle, and recovery requirements
- [x] Add protocol-neutral device identity model and key-manager boundary
- [x] Define explicit read-versus-create identity operations and typed lifecycle failures
- [x] Document reinstall and missing-key recovery constraints
- [x] Add pure identity-artifact reconciliation policy and unit tests
- [x] Add protocol-neutral enrollment-request lifecycle model and unit tests
- [x] Define proposed role-permission matrix and pure policy unit tests
- [x] Add explicit domain authorization decisions and typed policy-denial result
- [x] Define authorization-service boundary that resolves membership by device identity (contract and tests only)
- [x] Add fail-closed default membership resolver and tests
- [x] Make authorization fail closed on resolver errors and preserve coroutine cancellation
- [x] Record proposed device-authentication signature profile (P-256 ECDSA/SHA-256 with COSE ESP256); implementation review gates remain open
- [x] Add signed device-enrollment request envelope with nonce and expiry binding
- [x] Add public-key signature verification and tamper/expiry tests
- [x] Add durable one-time nonce-store contract and Room implementation with schema migration
- [x] Add fail-closed proof acceptance service and replay tests
- [x] Add bounded, versioned text export/import codec for signed enrollment proofs and public identities
- [x] Persist verified incoming enrollment requests atomically with nonce consumption and expose a local review inbox
- [x] Add explicit, one-per-family local initial-owner bootstrap bound to the current device key
- [x] Add signed, bounded initial-owner trust bundle export/verification primitives (manual out-of-band fingerprint check still required)
- [x] Add explicit recipient-side fingerprint confirmation and immutable local owner-key pinning
- [x] Add signed, bounded owner decision artifacts bound to the exact family, request, applicant key, and pinned owner key (portable statement only; no membership grant)
- [x] Persist verified owner decisions atomically and close the matching pending inbox request (no member creation)
- [x] Add applicant-signed acceptance bound to the exact approved owner decision
- [x] Verify owner and applicant signatures on the owner device before finalization
- [x] Atomically persist an authenticated family-device record and consume the pending request
- [x] Expose authenticated family-device records for the selected family in the home screen
- [ ] Complete protocol review for canonical encoding, key identifiers, signature canonicality, and persistent replay protection
- [x] Implement Android Keystore P-256 signing-key storage and public identity retrieval
- [x] Add explicit local device-identity setup and protection-level status to the home screen
- [ ] Define trusted initial-owner ceremony and ownership transfer
- [ ] Define trusted device enrollment, revocation, and recovery
- [ ] Implement invitation lifecycle only after protocol review
- [ ] Threat-model identity and enrollment before implementing network behavior

## Milestone 2 — Secure messaging foundations

Select and document reviewed cryptographic building blocks, key lifecycle, and message envelope formats before implementation.

## Milestone 3 — Connectivity and synchronization

Design peer discovery, transport abstraction, offline behavior, revocation propagation, and synchronization. Avoid treating transport security as a replacement for end-to-end encryption.

## Milestone 4 — Product experience

Build family spaces, conversations, delivery states, accessibility, and localization.

## Milestone 5 — Hardening and release

Expand automated tests, threat-model review, privacy review, release signing, and operational documentation.

## Current work package

**WP — Identity and trusted enrollment protocol review.** The local proof,
decision, acceptance, and owner-side finalization flow is implemented and
covered by unit tests. Do not begin network transport or claim production
membership authorization until the protocol review gates are closed.

### Validated on the current integration candidate

PR #2 head `8f94a1a3dc26e124a748212077b6e70bdfff44f6` passed Android CI run
[#45760437373](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45760437373)
on 2026-11-26. All three jobs passed:

- `verify`: COSE primitive KAT, 15 backup-policy regression tests, direct
  backup-policy verification, JVM unit tests, aggregate Android Lint,
  Debug APK assembly, Room schema export, and artifact uploads.
- API 31 identity/Keystore instrumentation: 12/12 passed.
- API 34 storage instrumentation: 9/9 passed; API 34 identity/Keystore
  instrumentation: 12/12 passed.
- The uploaded instrumentation reports were inspected; 33 instrumented
  cases completed with no failures, errors, or skips.
- Lint reported 13 warnings in `:app` and zero in core modules: five
  `NewerVersionAvailable`, four `GradleDependency`, three
  `AndroidGradlePluginVersion`, and one `OldTargetApi`.

This validates the named candidate commit and emulator images only. It does
not prove physical-device compatibility, hardware-backed Keystore assurance,
remote attestation, or security against a compromised operating system.
PR #2 remains Draft and unmerged; `main` has not received these changes.

### Remaining protocol and product gates

- [ ] Select canonical interoperable COSE_Key / COSE_Sign1 wire representation
- [x] Add first reproducible primitive COSE v2 known-answer vector (one artifact context; not full interoperability approval)
- [x] Reconstruct primitive KAT 0001 in a separate Python-standard-library/OpenSSL verifier (fixture-level check only; not a second complete CBOR/COSE implementation)
- [ ] Produce complete schemas and test vectors for all four enrollment artifacts
- [ ] Define strict ECDSA-to-COSE signature conversion and low-S policy
- [ ] Finalize versioned public-key serialization and key-ID derivation (current v1 remains SHA-256 over X.509 SPKI with domain separation)
- [ ] Resolve the gap between the proposed COSE ESP256 profile and current JCA ECDSA / X.509 / custom length-prefixed payload implementation
- [ ] Define replay protection and nonce retention across multiple devices
- [ ] Define trusted owner ceremony, ownership transfer, revocation, and recovery
- [ ] Threat-model the complete enrollment flow, including local database tampering, clock rollback, compromised devices, and offline peers
- [x] Add Android API 31+ Keystore instrumentation coverage (current PR #2 run #45760437373: API 31 identity 12/12; API 34 identity 12/12)
- [x] Add `MigrationTestHelper` coverage for Room start versions 1–7, representative row preservation, and final SQLite constraints
- [x] Pass the fixture-backed Room migration suite on API 34 (run #43231837501: 7 tests, 0 failures/errors/skips; v1–v7 migration and preservation assertions passed)
- [x] Execute deterministic concurrent DAO coverage for same-artifact imports with different receipt times and concurrent opposite outcomes (run #43602180283 passed; tests characterize current DAO outcomes only and do not choose product idempotency/conflict policy)

A draft COSE v2 profile and migration considerations are documented in
ADR 0003. Its proposals are not production decisions until interoperability
vectors, platform tests, and security review are complete.

### Next implementation sequence

1. Review the four signed enrollment artifact formats side-by-side and create
   a field/validation parity matrix against the draft profile.
2. Resolve the clock, expiry, nonce-retention, and replay semantics in ADR 0006.
3. Resolve authoritative owner-decision issuance and exact FMJA1 replay
   identity before designing a Room v8 migration.
4. Complete the threat model and independent interoperability review.
5. Only after those gates are approved, implement the selected protocol and
   corresponding migration/tests. Keep network transport and production
   authorization disabled until then.


## 2026-11-27 — Current integration candidate validation

PR #2 head `09eaa3832ac50e13b27506953d310adfae4b2f46` passed Android CI [run #45885409892](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45885409892) on
2026-11-26. All three jobs passed: `verify`, API 31 identity/Keystore
instrumentation, and API 34 storage/identity instrumentation. The verify job
completed the COSE primitive KAT, 15 backup-policy tests, direct policy
verification, JVM unit tests, aggregate Lint, Debug APK assembly, Room schema
export, and artifact uploads.

The downloaded Lint XML reports contain 13 warnings in `:app` and zero in
all four core modules: five `NewerVersionAvailable`, four
`GradleDependency`, three `AndroidGradlePluginVersion`, and one
`OldTargetApi`. No `ObsoleteSdkInt` warning remains.

This validates the named candidate and CI emulator images only. PR #2 remains
Draft and unmerged; `main` is unchanged. It does not establish hardware-backed
Keystore assurance, remote attestation, physical-device compatibility, or
production protocol approval.
