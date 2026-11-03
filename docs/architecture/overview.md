# Architecture Overview

## Goals

FamilyMesh is an Android-native, privacy-focused family communication app.
The codebase uses explicit boundaries so transport, identity, cryptography,
persistence, and UI can evolve independently.

## Current module boundaries

- **app** — Android application entry point, application lifecycle,
  dependency graph, Compose UI, and navigation host.
- **core:common** — platform policy, family domain models, authorization
  contracts, enrollment state, and repository interfaces.
- **core:storage** — local persistence adapters. Room stores family spaces,
  local member metadata, owner trust, pending requests, decisions, and
  authenticated-device records; Preferences DataStore supports small settings.
- **core:designsystem** — shared Compose theme and visual foundations.
- **core:identity** — Android Keystore device signing, public identity,
  enrollment proof/decision/acceptance codecs and verifiers, and local
  finalization services.

## Dependency direction

Feature and application layers may depend on core abstractions. Core modules
must not depend on the app module. Cross-module dependencies should be
introduced only when a concrete use case needs them.

## Security baseline and implementation boundary

- Minimum supported Android API: 31 (Android 12).
- Cleartext network traffic is disabled in the application manifest.
- Android backup is disabled until a reviewed backup and key-recovery design
  exists.
- The current identity provider creates non-exportable P-256 EC signing keys
  in Android Keystore and exposes public X.509 Subject Public Key Info bytes.
- The local enrollment prototype supports signed device requests, local
  initial-owner trust and pinning, signed owner decisions, applicant-signed
  acceptance, and owner-side local finalization.
- The current signed payloads use fixed-order, length-prefixed application
  encodings and manual text envelopes. The proposed COSE ESP256 profile has
  not yet been implemented as a canonical COSE wire format.
- No end-to-end message encryption, peer discovery, BLE, Wi-Fi Direct,
  internet P2P, relay, cross-device synchronization, production membership
  resolver, revocation, or recovery protocol is implemented.
- The current Room records are local to one installation. An authenticated
  device record is not yet a distributed or globally current membership proof.

## Persistence

Preferences DataStore is provided for small settings. Room stores family
spaces and local member-list metadata as well as the local identity/enrollment
workflow records. The local member-list metadata is not authenticated
membership. Owner-side authenticated-device records are produced only after
the local proof/decision/acceptance checks; they are not synchronized to other
devices and are not yet consumed by a production authorization resolver.

## Dependency injection

Koin initializes at the application boundary. Modules expose dependencies
through constructor injection rather than reaching into global state.
