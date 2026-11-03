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
- [ ] Select and review cryptographic identity protocol and public-key format
- [ ] Implement and test Android-backed key storage after protocol review
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
