# ADR 0014 — Enrollment Persistence Test Coverage Audit

- Status: Test-coverage audit / proposed validation work; no runtime behavior approved
- Date: 2026-11-03
- Scope: identity unit tests, Room persistence transactions, migration validation, and CI
- Related: ADR 0008, ADR 0009, ADR 0011, ADR 0012, ADR 0013

## Source-backed current state

The current CI workflow `.github/workflows/android-ci.yml` runs only:

1. `gradle testDebugUnitTest`;
2. `gradle assembleDebug`;
3. debug APK artifact upload.

It does not run `connectedAndroidTest`, Room migration tests, or another instrumentation task.

The `:core:storage` module currently declares Room runtime/compiler dependencies but no test dependencies. The repository tree contains no `:core:storage/src/test` or `:core:storage/src/androidTest` source set. `FamilyMeshDatabase` has `exportSchema = false`, so checked-in Room schema snapshots are not available for migration review.

The identity tests `EnrollmentFinalizationServiceTest` and `EnrollmentDecisionAcceptanceServiceTest` exercise service logic using in-memory fake repositories. They cover useful cryptographic and domain outcomes, but do not execute the Room DAO or SQLite transaction implementation.

## Persistence behaviors verified by source, but not by SQLite tests

The current DAO in `FamilyMeshDatabase.kt` implements transactional methods for:

- nonce consumption and pending-request insertion;
- applicant-side decision insertion and pending-request deletion;
- owner-side membership insertion and pending-request deletion.

These methods use Room `@Transaction`, but current CI does not exercise their rollback behavior, unique-index races, foreign-key behavior, or persistence across database close/reopen.

In particular, the current tests do not prove:

- concurrent calls against the same request produce one deterministic database winner;
- an exception between related writes rolls back all writes;
- exact persisted artifacts survive process/database recreation;
- migration from each supported historical schema preserves rows and constraints;
- owner and applicant repositories backed by separate database instances remain isolated;
- a future owner-issued decision ledger and FMJA1 replay digest are atomically enforced together.

## Required implementation/test sequence

Before implementing the schema proposal in ADR 0012:

1. Add `:core:storage` test dependencies and a deterministic in-memory Room test fixture.
2. Add DAO-level tests for decision persistence, finalization, nonce consumption, uniqueness, foreign keys, rollback, and reopen durability.
3. Add migration fixtures for every supported starting version (1 through 7) and verify preservation of representative rows and indexes.
4. Add instrumentation coverage on a pinned emulator/API image for migration and SQLite concurrency behavior.
5. Publish test reports and diagnostics on failure; make the instrumentation job a required CI check before schema rollout.
6. Keep identity service unit tests; they complement rather than replace persistence tests.

When ADR 0008/0012 are approved, extend the same suite with first-issuance wins, exact reissue returns stored FMOD1, conflicting issuance fails, only exact stored approval finalizes, rejection/unrecorded approval fails closed, exact FMJA1 replay behavior, legacy NULL digest behavior, restart recovery, and concurrent opposite-outcome issuance.

## Acceptance gates

- Room DAO behavior is exercised against SQLite, not only fakes.
- Every supported migration path is tested from a real historical schema fixture.
- Transaction rollback, unique constraints, and foreign-key cascade are asserted.
- Reopen/restart tests verify durable owner artifact and membership state.
- CI runs and reports instrumentation separately from unit tests and APK assembly.
- No test assumes a selected expiry policy until ADR 0006 is resolved.

## Non-goals

This audit does not add test dependencies, alter CI, change Room schema, implement the owner ledger, select expiry semantics, or modify protocol, cryptography, transport, or runtime behavior.


## Additional concurrency case found during follow-up review

The applicant-side `EnrollmentNonceDao.persistDecision` transaction compares an existing `EnrollmentDecisionEntity` with the incoming entity using full data-class equality. The entity includes `recordedAtEpochMillis`, which is the local import/recording time rather than part of the owner's signed FMOD1 artifact.

The service's sequential replay path checks the signed decision fields and intentionally does not compare the local recording time. However, two concurrent imports of the same signed FMOD1 can both initially observe the pending request, construct records with different local `recordedAtEpochMillis` values, and then serialize at the Room transaction boundary. The first can record; the second can observe the row but compare unequal because its local recording time differs, returning `CONFLICT`.

The intended result for this race should be explicitly decided and tested. If exact signed-artifact re-import is meant to be idempotent regardless of local receipt time, persistence equality should distinguish signed artifact identity from observer metadata. Do not silently change this behavior as part of the owner-issued ledger work; keep applicant import semantics separately reviewed.

Add a deterministic concurrent same-artifact import test with distinct local receipt times, alongside concurrent opposite-artifact conflict coverage.


## 2026-11-03 follow-up — instrumentation baseline and migration provenance

The initial Room/SQLite baseline is now present in the repository:

- `:core:storage` has Android instrumentation dependencies and an AndroidJUnitRunner.
- `EnrollmentNonceDaoInstrumentedTest` covers nonce replay rejection, applicant decision persistence/replay/conflict, owner finalization/replay/conflict, nonce rollback when pending insertion fails, and file-backed close/reopen durability.
- `.github/workflows/android-ci.yml` has a separate `storage-instrumentation` job that invokes `gradle :core:storage:connectedDebugAndroidTest` and attempts to upload reports even after failure.
- This is five DAO tests, not comprehensive persistence validation. Foreign-key cascade, historical migration preservation, and deterministic concurrent races remain uncovered.

### CI result and emulator diagnosis

The `verify` job passed in run #41693826071, but `storage-instrumentation` did not reach Gradle or execute any test. The Android Emulator process was launched with API 35 / Google APIs / x86_64 and SwiftShader, but `sys.boot_completed` never became true; the runner ended with `Timeout waiting for emulator to boot`. Emulator output also reported a missing color buffer. This is an environment boot failure, not a test assertion failure, and it provides no pass/fail evidence about the Room tests.

The workflow was adjusted in commit `49a7082ed14c6d415f2996682f8485c10cc587f2` to use API 34 and the SwiftShader renderer, while retaining the bounded boot timeout and diagnostics-oriented options. Its CI result must be checked before treating the emulator setup as resolved.

### Historical migration provenance

A Git-history audit of `FamilyMeshDatabase.kt` found committed source snapshots corresponding to Room versions 1 through 7. The historical entity sets corroborate the version progression:

- v1: family spaces and members;
- v2: consumed enrollment nonces;
- v3: pending enrollment requests;
- v4: initial owner trust;
- v5: trusted initial owners;
- v6: enrollment decisions;
- v7: authenticated family devices.

This establishes source provenance for the versioned entity evolution, but it does not by itself provide Room-exported historical schema JSON or prove every historical SQLite table/index/FK definition. `exportSchema` remains false. Do not create purported historical Room schema fixtures by guessing from current entities or by treating Git source snapshots as exported schema files. A migration test fixture must be generated from a reproducible historical build/schema export or another independently verifiable historical database artifact.

### Remaining acceptance work

1. Obtain a successful emulator boot and execute the existing instrumentation suite.
2. Establish reproducible, verifiable historical SQLite schemas for versions 1–7 before asserting migration preservation.
3. Test each supported migration path, representative row preservation, indexes, and foreign keys against those real fixtures.
4. Add foreign-key cascade tests and deterministic concurrency tests for same-artifact applicant imports with distinct local receipt times and conflicting owner outcomes, without changing runtime semantics until the relevant ADR decisions are approved.
