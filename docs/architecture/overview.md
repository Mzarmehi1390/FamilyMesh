# Architecture Overview

## Goals

FamilyMesh is an Android-native, privacy-focused family communication app.
The codebase starts with explicit boundaries so transport, identity,
cryptography, persistence, and UI can evolve independently.

## Initial module boundaries

- **app** — Android application entry point, application lifecycle, dependency graph, and navigation host.
- **core:common** — platform policy and shared domain primitives.
- **core:storage** — local persistence adapters. Room stores family spaces and local member-list metadata; Preferences DataStore supports small settings.
- **core:designsystem** — shared Compose theme and visual foundations.

## Dependency direction

Feature and application layers may depend on core abstractions. Core modules
must not depend on the app module. Cross-module dependencies should be
introduced only when a concrete use case needs them.

## Security baseline

- Minimum supported Android API: 31 (Android 12).
- Cleartext network traffic is disabled in the application manifest.
- Android backup is disabled until a reviewed backup and key-recovery design exists.
- No cryptographic protocol, trusted identity system, invitation protocol, key
  storage scheme, or peer discovery mechanism is implemented. These require
  dedicated design and review before production use.

## Persistence

Preferences DataStore is provided for small settings. Room stores family-space
records and local member-list entries. These records are device-local metadata;
they do not establish membership or authorization on another device.

## Dependency injection

Koin initializes at the application boundary. Modules expose dependencies
through constructor injection rather than reaching into global state.
