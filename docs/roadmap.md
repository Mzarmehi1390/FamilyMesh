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
- [ ] Verify build and tests on a clean Android/Gradle environment
- [ ] Add and validate the Gradle Wrapper

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
- [x] Add explicit, one-per-family local initial-owner bootstrap bound to the current device key\n- [x] Add signed, bounded initial-owner trust bundle export/verification primitives (manual out-of-band fingerprint check still required)
- [x] Add explicit recipient-side fingerprint confirmation and immutable local owner-key pinning
- [ ] Complete protocol review for canonical encoding, key identifiers, and persistent replay protection
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
