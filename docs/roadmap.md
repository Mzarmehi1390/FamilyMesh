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
- [x] Verify unit tests and debug APK on the clean GitHub Actions Ubuntu/JDK 17/Gradle 8.13 environment (latest verified run: 2026-11-03)
- [x] Add and validate the official Gradle 8.13 Wrapper (JAR and distribution checksums pinned; PR #2 CI run #43231837501 passed)

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

Required gates:
- [x] Review current bounded parsing and reject non-canonical Base64URL aliases across all four transfer envelopes
- [x] Enforce P-256-only public-key parsing at all enrollment signature-verification boundaries
- [ ] Select canonical interoperable COSE_Key / COSE_Sign1 wire representation
- [x] Add first reproducible primitive COSE v2 known-answer vector (one artifact context; not full interoperability approval)
- [ ] Define strict ECDSA-to-COSE signature conversion and low-S policy
- [ ] Finalize versioned public-key serialization and key-ID derivation (current v1 remains SHA-256 over X.509 SPKI with domain separation)
- [ ] Resolve the gap between the proposed COSE ESP256 profile and current JCA ECDSA / X.509 / custom length-prefixed payload implementation
- [ ] Define replay protection and nonce retention across multiple devices
- [ ] Define trusted owner ceremony, ownership transfer, revocation, and recovery
- [ ] Threat-model the complete enrollment flow, including local database tampering, clock skew, compromised devices, and offline peers
- [ ] Add Android API 31+ Keystore instrumentation coverage
- [x] Add `MigrationTestHelper` coverage for Room start versions 1–7, representative row preservation, and final SQLite constraints
- [x] Pass the fixture-backed Room migration suite on API 34 (run #43231837501: 7 tests, 0 failures/errors/skips; v1–v7 migration and preservation assertions passed)
- [ ] Execute deterministic concurrent DAO coverage for same-artifact imports with different receipt times and concurrent opposite outcomes

A draft COSE v2 profile and migration considerations are documented in
ADR 0003. Its proposals are not production decisions until interoperability
vectors, platform tests, and security review are complete.
