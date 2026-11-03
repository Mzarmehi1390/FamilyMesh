# FamilyMesh Work Log

## 2026-11-03 — Repository synchronization and documentation correction

### Baseline

- Repository: `Mzarmehi1390/FamilyMesh`
- Branch: `main`
- Baseline commit: `5d1afe394178ead84d8f54219e28ee3ccf7c1675`
- Latest CI run at baseline: #41667652277 — success.

### Verified

- Enumerated the complete repository tree (146 entries).
- Inspected module structure, Gradle configuration, Android manifest,
  workflow, roadmap, architecture docs, identity/enrollment services,
  Room persistence and migrations, and home-screen enrollment flow.
- Confirmed no open issues or pull requests at synchronization.
- Confirmed the latest CI run passed unit tests, debug APK assembly, and
  artifact upload.

### Findings

- The code had advanced beyond README, roadmap, architecture, and membership
  documentation.
- The current signed enrollment payloads are custom fixed-order,
  length-prefixed v1 encodings; ADR 0001's COSE ESP256 profile remains
  proposed, not implemented.
- Owner-side authenticated-device records are local only and are not yet
  consumed by a production authorization resolver.
- No E2EE messaging, P2P transport, relay, revocation, or recovery exists.
- Gradle Wrapper and Android instrumentation validation remain outstanding.

### Documentation changes

- Corrected README and architecture/roadmap status to match implementation.
- Documented applicant acceptance and owner-side finalization.
- Added project status and handover records.
- Recorded protocol review as the next work package.

### Tests

No tests were executed by this documentation-only synchronization. The
reported passing tests are from the already completed GitHub Actions run
#41667652277 for the baseline commit.

### Next action

Audit the signed enrollment chain and document a reviewed protocol decision
before implementing authorization integration or network transport.

## 2026-11-03 — Close owner-pin trust-boundary gap

### Finding

`EnrollmentDecisionAcceptanceService` previously accepted a
`TrustedInitialOwner` supplied by its caller. The current UI checked
the pin before calling the service, but the service boundary itself did not
guarantee that the supplied owner key came from the local pin repository.

### Change

- Injected `InitialOwnerPinRepository` into the acceptance service.
- The service now resolves exactly one pin for the decision's family before
  checking or persisting the decision.
- Removed the caller-supplied trust parameter from the service API and UI.
- Updated Koin wiring.
- Added three tests: missing pin, different pinned key, and ambiguous pins.

### Verification

- Commit: `60f02eea4abe2d6ff8d9fe1d4c404724f049c1a8`
- CI run: [#41670679347](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41670679347)
- Unit tests: passed.
- Debug APK assembly: passed.
- Artifact upload: passed.

### Remaining limitation

This closes the caller-injected trust-record gap. It does not protect against
compromise or arbitrary modification of the local application database and
does not replace an independently reviewed trust protocol.

## 2026-11-03 — Enrollment protocol boundary hardening

### Canonical transfer encoding

Audit confirmed that all four text envelope decoders used Java's URL-safe
Base64 decoder directly. The encoders emitted unpadded Base64URL, but the
decoders did not enforce that single representation. Added
`CanonicalBase64Url`, which checks the URL-safe alphabet, rejects padding,
decodes, and round-trips to require exact canonical representation. Applied
it to FMER1, FMOT1, FMOD1, and FMJA1. Added tests for canonical input,
padded aliases, and non-URL-safe alphabet input.

### Public-key algorithm constraints

The Android Keystore generator creates P-256 keys, and ADR 0001 proposes
P-256/SHA-256, but the verification paths previously accepted any EC curve
supported by the JCA provider. Added `P256PublicKeyParser` and applied it to
proof, owner trust bundle, owner decision, and applicant acceptance
verification. Added P-384 rejection and P-256 acceptance tests.

### Verification

- Canonical Base64URL commit: `918b03b75181f6aa541e5df296cc6ad8164c84e6`
  — CI #41671671596 passed.
- P-256 verification commit: `36e2d27c0fcac8defe36a116fdd6792e071f011a`
  — CI #41671944679 passed.
- Parser block-body cleanup: `21dd7ca4db718ee31416e45319d88ffce9c950c3`
  — CI #41671995919 passed unit tests, APK assembly, and artifact upload.

### Remaining protocol decisions

COSE_Key serialization, canonical key ID, DER-to-COSE signature conversion,
low-S policy, cross-language vectors, maximum request lifetime, clock skew,
distributed replay/freshness, trust ceremony, revocation, and recovery remain
open. The local custom format is still not a production protocol.

## 2026-11-03 — Protocol profile research and handover refresh

### Standards review

- Confirmed RFC 9864 (October 2025) registers ESP256 as COSE algorithm -9,
  fully specifying ECDSA with P-256 and SHA-256, and marks polymorphic COSE
  ES256 (-7) deprecated.
- Confirmed RFC 9053 encodes ECDSA signatures as fixed-width R || S, each
  coordinate padded to the curve byte width (32 bytes for P-256).
- Confirmed RFC 9052 COSE_Sign1 signs the Sig_structure with context
  "Signature1", protected-header bytes, external AAD, and payload.
- Confirmed RFC 9053 requires EC2/P-256 key type/curve consistency.
- Confirmed RFC 8949 core deterministic CBOR uses preferred serialization,
  forbids indefinite-length items, and sorts map keys by bytewise
  lexicographic order of deterministic encodings.

### Documentation work

- Updated ADR 0001 to cite RFC 9864 and distinguish ESP256 from deprecated
  polymorphic ES256.
- Added ADR 0003 as a draft v2 COSE profile, with explicit proposals for
  key encoding, key IDs, protected headers, external AAD, signature
  conversion, and migration boundaries.
- Refreshed roadmap, handover, and project status to reflect the verified
  CI state and current source/documentation distinction.
- Corrected the stale literal "\\n" formatting defect in the roadmap.

### Verification

Documentation changes are being committed as a cohesive update. CI must pass
on the resulting commit before this documentation package is considered
verified. No cryptographic protocol implementation or authorization/
networking behavior was changed in this work package.
