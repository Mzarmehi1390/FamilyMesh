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
`TrustedInitialOwner` supplied by its caller. The current UI checked the
pin before calling the service, but the service boundary itself did not
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
