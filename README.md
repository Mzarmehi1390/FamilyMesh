# FamilyMesh

Private, decentralized, end-to-end encrypted family messaging for Android.

## Project status

Milestone 0 (native Android foundation) is implemented. Milestone 1 (domain,
device identity, and trusted enrollment foundations) is in progress. The
repository currently contains a local, manually transferred enrollment
prototype: device-key-signed requests, locally pinned initial-owner trust,
signed owner decisions, applicant-signed acceptance, and owner-side local
membership finalization.

This is **not** a production-ready messaging system. There is no end-to-end
message encryption, peer-to-peer transport, relay, cross-device membership
synchronization, production authorization resolver, device revocation, or
recovery protocol yet. See the [project status](PROJECT_STATUS.md),
[handover](HANDOVER.md), and [roadmap](docs/roadmap.md).

## Platform and stack

- Minimum OS: Android 12 (API 31)
- Kotlin + Jetpack Compose
- Gradle Kotlin DSL and Version Catalog
- Modular structure: `app`, `core:common`, `core:storage`,
  `core:designsystem`, `core:identity`
- Koin for dependency injection
- Preferences DataStore for small settings
- Room for local family, trust, enrollment, and authenticated-device records
- JDK 17 toolchain

## Build

Open the repository in a current Android Studio installation with Android SDK
36 installed. The checked-in Gradle Wrapper pins Gradle 8.13 and verifies the
downloaded distribution with its published SHA-256 checksum.

On macOS/Linux:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

GitHub Actions uses the same checked-in wrapper with JDK 17. Successful runs
publish the `familymesh-debug-apk` artifact for seven days.

## Security notes

Cleartext traffic and Android automatic backup are disabled by default.
Device signing keys are generated in Android Keystore using P-256 ECDSA and
SHA-256. The current enrollment exchange uses bounded, versioned,
length-prefixed application payloads and manual text transfer; it is not yet
the proposed interoperable COSE wire format. Protocol review is still
required for canonical encoding, key identifiers, signature representation,
replay protection, trust ceremony, revocation, and recovery. A concrete but
unapproved COSE v2 proposal is recorded in
[ADR 0003](docs/architecture/decisions/0003-cose-v2-enrollment-profile-draft.md).

All enrollment state described here is local to an individual installation.
It must not be interpreted as cross-device authorization. No end-to-end
messaging encryption or network transport is implemented.

See [Architecture](docs/architecture/overview.md),
[Device identity and trust](docs/architecture/device-identity-and-trust.md),
[Membership](docs/architecture/membership-and-invitations.md), and
[Roadmap](docs/roadmap.md).
