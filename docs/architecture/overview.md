# Architecture Overview

## Goals

FamilyMesh is an Android-native, privacy-focused family communication app. The codebase starts with explicit boundaries so transport, identity, cryptography, persistence, and UI can evolve independently.

## Initial module boundaries

- **app** — Android application entry point, application lifecycle, dependency graph, and navigation host.
- **core:common** — platform policy and small shared primitives.
- **core:storage** — local persistence adapters. Room is included for structured data; Preferences DataStore is initialized for small settings.
- **core:designsystem** — shared Compose theme and visual foundations.

## Dependency direction

Feature and application layers may depend on core abstractions. Core modules must not depend on the app module. Cross-module dependencies should be introduced only when a concrete use case needs them.

## Security baseline

- Minimum supported Android API: 31 (Android 12).
- Cleartext network traffic is disabled in the application manifest.
- Android backup is disabled until a reviewed backup and key-recovery design exists.
- No cryptographic protocol, key storage scheme, or peer discovery mechanism is claimed to be implemented in Milestone 0. Those require dedicated design and review before production use.

## Persistence

Preferences DataStore is provided as a small-settings foundation. Room runtime dependencies are present, but the database, entities, migrations, and DAOs will be added with the first concrete persistence use case.

## Dependency injection

Koin initializes at the application boundary. The initial module provides the preferences adapter. Modules should expose dependencies through constructor injection rather than reaching into global state.
