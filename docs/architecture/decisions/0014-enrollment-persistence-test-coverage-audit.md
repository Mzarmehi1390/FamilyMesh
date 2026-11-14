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
- This is six DAO tests, not comprehensive persistence validation. The sixth test covers authenticated-device uniqueness and family-delete cascade. Historical migration preservation and deterministic concurrent races remain uncovered.

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


### 2026-11-04 follow-up — API 34 attempt and six-test inventory

The latest main-branch run, #41697147520 for commit `e945b8774d8d43d8426e8f2c2f4f541800d1d2cb`, completed with:

- `verify`: success (unit tests, debug APK assembly, APK artifact upload);
- `storage-instrumentation`: failure before Gradle/test execution.

The API 34 / Google APIs / x86_64 / Pixel 2 emulator using SwiftShader did not report `sys.boot_completed=1` within the configured 600-second timeout. The runner log shows repeated ADB exit-code-1 polling after emulator launch, followed by `Timeout waiting for emulator to boot`. No instrumentation report was generated. The earlier API 34 run #41696869594 failed at the same boot boundary. These are environment startup failures, not test assertion failures, and provide no pass/fail evidence for Room DAO behavior.

The instrumentation source now contains six tests. In addition to the five baseline cases, `sqliteConstraints_enforceDeviceUniquenessAndFamilyCascade` asserts that duplicate authenticated-device keys are rejected and deleting a family cascades to pending enrollment and authenticated-device rows. This sixth test has not been executed successfully; the current `verify` job does not compile Android instrumentation sources, so its compile/runtime status remains unverified.

Run #41697076341 for commit `98c1409b` was cancelled as the subsequent main push began. This is consistent with the next push superseding the prior run and does not prove an issue in the conditional concurrency setting.

### Revised acceptance state

- JVM unit tests and debug APK assembly: verified on #41697147520.
- Android instrumentation source and all six SQLite cases: present, execution unverified.
- Emulator boot: blocked on API 34 in two consecutive attempts.
- Historical migration fixtures for v1–v7: still unavailable; no guessed schema fixtures added.
- Runtime, Room schema, protocol, cryptography, and transport: unchanged.


### 2026-11-04 follow-up — successful instrumentation and v7 schema snapshot

Run #41813611838 (#172), commit `668542632261dac50d98915e76765a5125a4c76f`, completed successfully:

- `verify`: success (JVM unit tests, debug APK assembly, artifact upload).
- `storage-instrumentation`: success on API 34 emulator.
- Instrumentation log: 6 tests started, 6/6 completed, 0 skipped, 0 failed; Gradle reported `BUILD SUCCESSFUL`.
- The immediate preceding instrumentation failure was traced to the test APK lacking `androidx.test.runner.AndroidJUnitRunner`. The version catalog and storage module now explicitly include `androidx.test:runner:1.7.0`.

The schema artifact upload initially exposed a separate gap: the regular verify tasks did not leave a schema JSON at the configured path, and the upload step only warned. The workflow now explicitly runs `gradle :core:storage:kspDebugKotlin --rerun-tasks` before upload and treats a missing schema file as an error.

Run #41818762325 (#173), commit `da5e86a94937fcbbc9d0a221b8a2d0c091a50358`, completed its verify job successfully, including explicit KSP generation and schema artifact upload. Artifact #13316525198 was downloaded and inspected. It contains:

- `com.familymesh.core.storage.family.FamilyMeshDatabase/7.json`;
- Room schema version 7;
- identity hash `3d3bfeb59dfda625d9f6664d1c39765b`;
- eight entities, including `authenticated_family_devices`;
- unique composite index `index_authenticated_family_devices_familySpaceId_deviceKeyId` on `familySpaceId, deviceKeyId`;
- the exported primary keys, indices, and foreign-key declarations for the current v7 entities.

This is the first actual exported Room schema snapshot. It is a current v7 schema baseline only; it is not a historical v1–v6 fixture and does not establish migration correctness. Run #173's instrumentation job remained in progress at the time of this update.

### Revised acceptance state

- Six existing Room DAO instrumentation tests: executed successfully on API 34 in run #172.
- JVM unit tests and debug APK assembly: successful in runs #172 and #173 verify.
- Current Room v7 schema JSON: generated, uploaded, downloaded, and structurally inspected from run #173.
- Historical schema fixtures and migration validation for v1–v6: still outstanding.
- Deterministic concurrency coverage for same-artifact applicant imports and opposite owner outcomes: still outstanding.
- No runtime/protocol/cryptography/transport behavior or Room schema version change was made.


### 2026-11-04 follow-up — run #173 instrumentation confirmed; #175 emulator retry

Run [#41818762325](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41818762325) (#173) completed both CI jobs successfully. The instrumentation artifact #13317910182 was downloaded and its JUnit XML inspected directly:

- Test suite: `EnrollmentNonceDaoInstrumentedTest`
- Device: API 34 emulator
- Tests: 6
- Failures: 0
- Errors: 0
- Skipped: 0
- Suite execution time: 72.646 seconds

This independently confirms that all six current Room DAO tests execute successfully, including rollback, close/reopen durability, uniqueness, and family cascade. This does not validate migrations.

Run [#41819471027](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41819471027) (#175), at commit `b0879b22cf97295109a35c5388c43feb0105a346`, passed `verify`, including Room v7 schema generation and artifact upload. Its instrumentation job failed before Gradle/test execution because the emulator did not boot within the configured 600-second timeout. The failed job was re-run independently; that retry is still in progress. Treat this as an environment startup failure, not a test failure, and do not report the retry as green until completed.

The historical Room schema export workflow for v1–v7 is committed but has not yet been dispatched. Actual historical fixtures and `MigrationTestHelper` validation remain the next migration gate. No runtime/schema/protocol/crypto/transport changes were made.


### 2026-11-04 follow-up — run #176 instrumentation report inspected

Run [#41828148184](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41828148184), commit `f2b2988b0a4ffd83784cd3f87b2f2c0fd2a0e082`, completed both CI jobs successfully. The instrumentation artifact #13325065058 was downloaded and its JUnit XML parsed directly:

- Suite: `EnrollmentNonceDaoInstrumentedTest`
- Device: API 34 emulator
- Tests: 6
- Failures: 0
- Errors: 0
- Skipped: 0
- Suite execution time: 129.86 seconds

All six DAO cases ran, including SQLite uniqueness/cascade, atomic nonce consumption, conflicting replay rejection, transaction rollback, database reopen durability, and applicant decision persistence. This confirms the prior emulator startup timeout was transient. It does not validate historical migrations: authentic v1–v7 schema fixtures and MigrationTestHelper coverage remain outstanding. No runtime, Room schema, protocol, cryptography, expiry, or transport behavior changed.


### 2026-11-13 follow-up — historical migration validation completed on PR #2

- Combined branch head: `2897397fa2e9f672f891c2f2f61f26ada901a372`.
- Android CI run [#43231837501](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/43231837501) completed both `verify` and `storage-instrumentation` successfully.
- Downloaded instrumentation artifact #13952322275 and inspected its JUnit XML directly: 7 tests, 0 failures, 0 errors, and 0 skipped on the API 34 emulator.
- The six DAO cases passed. The migration case `everySupportedStartVersion_migratesToV7AndPreservesRowsAndConstraints` iterated every supported starting schema (v1–v7), seeded representative rows, migrated to v7, checked row preservation and version-appropriate table contents, verified expected indexes and the unique device index, and exercised foreign-key cascades (including the intentional v5 trusted-owner exception).
- This closes the historical Room v1–v7 migration-validation gate for the current v7 migration chain on the tested API 34 image. It does not approve or validate a v7→v8 migration.
- The Gradle 8.13 wrapper, JVM unit tests, Debug APK assembly, and current schema export also passed in the same run.
- The DAO concurrency characterization cases for same-artifact/different-receipt-time imports and opposite outcomes are now added in the same review branch; their execution is pending the follow-up CI run.
- No production runtime, Room entity/schema version, protocol, cryptography, expiry policy, or transport behavior changed.
