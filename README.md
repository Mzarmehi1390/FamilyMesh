# FamilyMesh

Private, decentralized, end-to-end encrypted family messaging for Android.

## Project status

Milestone 0 establishes the native Android foundation. The repository is being built from scratch.

## Platform and stack

- Minimum OS: Android 12 (API 31)
- Kotlin + Jetpack Compose
- Gradle Kotlin DSL and Version Catalog
- Modular structure: `app`, `core:common`, `core:storage`, `core:designsystem`
- Koin for dependency injection
- Preferences DataStore for small settings
- Room runtime foundation for structured local data
- JDK 17 toolchain

## Build

Open the repository in a current Android Studio installation with Android SDK 36 installed. The Gradle Wrapper will be added and verified in the next setup pass; until then, a compatible local Gradle 8.13 installation can run:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

## Security notes

Cleartext traffic and Android automatic backup are disabled by default. This milestone does not yet implement end-to-end encryption, identity, or peer-to-peer transport. Those components will be designed and reviewed in later milestones.

See [Architecture](docs/architecture/overview.md) and [Roadmap](docs/roadmap.md).
