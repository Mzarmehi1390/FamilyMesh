# FamilyMesh Project Status

Last verified: 2026-11-04
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Branch: `main`
Latest repository commit: `7112f0ae2bfbb36a37e186b3a9eb4775d3e81213` (historical schema audit documentation; latest successful full CI baseline remains run #176)
Protocol review drafts: ADR 0003 and ADR 0006 (proposals only; not production-approved)

## Overall state

Milestone 1 — Domain and identity is in progress. The Android foundation is
in place and a local manually transferred device-enrollment prototype now
reaches owner-side authenticated-device persistence.

This is not a production-ready messenger. End-to-end message encryption,
peer-to-peer transport, relay, synchronization, revocation, recovery, and
production authorization are not implemented.

## Work package classification

| Area | Status | Evidence / limitation |
|---|---|---|
| Native Android + Compose foundation | IMPLEMENTED_AND_TESTED | CI builds debug APK |
| Modular Gradle architecture | IMPLEMENTED_AND_TESTED | Five modules present |
| Room / DataStore local storage | IMPLEMENTED_AND_INSTRUMENTED | Room schema v7; six DAO tests passed on API 34 in CI run #173; historical migrations remain unverified |
| Android Keystore device signing | IMPLEMENTED_UNVERIFIED | P-256-only verification enforced; unit tests; device/API instrumentation pending |
| Signed enrollment proof | IMPLEMENTED_AND_TESTED | Bounded local codec and verifier tests |
| Local nonce replay protection | IMPLEMENTED_AND_TESTED | One database only; not distributed |
| Initial-owner trust bootstrap/pinning | IMPLEMENTED_AND_TESTED | Manual out-of-band fingerprint comparison |
| Signed owner decision | IMPLEMENTED_AND_TESTED | Service resolves the pin itself; tests reject missing, mismatched, and ambiguous pins |
| Applicant acceptance + owner finalization | IMPLEMENTED_AND_TESTED | Both signatures verified; local Room v7 record |
| Production membership authorization | BLOCKED | No trusted resolver; default denies all |
| Canonical COSE protocol | IN_PROGRESS | Draft v2 profile, one primitive known-answer fixture, schema-parity review, and source-backed v1 field inventory and v2 time/text proposal (including signed-event vs local-receipt semantics and pending-expiry lifecycle gap) documented; production implementation and independent interoperability validation not complete |
| Invitations / revocation / recovery | NOT_STARTED | Requires reviewed lifecycle and authority model |
| E2EE messaging | NOT_STARTED | No message crypto implementation |
| BLE / Wi-Fi Direct / internet P2P / relay | NOT_STARTED | No transport implementation |
| Cross-device synchronization | NOT_STARTED | Manual text transfer only |
| Gradle Wrapper | NOT_STARTED | CI installs Gradle 8.13 |
| Android instrumentation / device validation | PARTIALLY_VERIFIED | Six Room DAO tests passed on API 34 in runs #172, #173, and #176; historical migrations and device-specific validation remain outstanding |

## Latest verification

CI run [#41672460808](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41672460808)
completed successfully for the documentation baseline:
- `testDebugUnitTest`
- `assembleDebug`
- debug APK artifact upload

This is CI evidence only; it is not a production security review or device
instrumentation result. Documentation commits after the last verified baseline are not covered by
that successful run. The latest lifecycle-audit commit is 86cc551; Android CI
run #41677314967 was queued/in progress at the last check, so its result is
not yet confirmed.

## Immediate priority

Complete the Identity & Trusted Enrollment protocol review. Keep authorization
integration and network transport blocked until canonical encoding, key IDs,
signature representation, replay scope, trust ceremony, and recovery/
revocation semantics have been reviewed.

## Protocol audit progress

- ADR 0008 proposes a separate owner-issued FMOD1 ledger. The existing
  `enrollment_decisions` table is applicant-side acceptance state and must
  not be repurposed. Proposed lifecycle states and exact-artifact matching
  at FMJA1 finalization are documented; no runtime/schema change has been
  made. Expiry policy remains unresolved under ADR 0006.

- ADR 0007 traces the separate applicant and owner Room stores. Applicant-side
  decision import and owner-side finalization use independent local rows; the
  apparent same-database conflict is not real. A separate owner-inbox gap is
  confirmed: issuing FMOD1 does not persist/update owner-side status, so
  rejected requests remain shown as awaiting review. Proposed remediation
  and integration/Room instrumentation tests are documented; no runtime
  change has been made.

- Four signed v1 envelope decoders enforce canonical unpadded Base64URL.
- All four signature verification paths reject non-P-256 EC keys.
- Custom fixed-order payloads remain non-COSE and are not approved as the
  production wire format.
- Current key IDs are domain-separated SHA-256 fingerprints over X.509 SPKI;
  JCA ECDSA signatures remain DER encoded.
- Nonce replay storage is atomic but installation-local.
- ADR 0003 provides a concrete draft profile for COSE v2; it is not yet an
  accepted production protocol.


## Room validation and expiry-policy update — 2026-11-03

- ADR 0009 documents the verified Room schema v7 migration chain and the
  missing SQLite instrumentation coverage in CI. It proposes, but does not
  implement, an additive v7 → v8 migration for a future owner-issued
  decision ledger.
- ADR 0006 now explicitly compares owner-receipt-deadline and signed-event-
  deadline expiry policies, including offline-delivery consequences and
  required product/security decisions. Neither policy is selected.
- Corrected the malformed T3 formatting in ADR 0006.
- ADR 0009 CI #41678430461 and Work Log CI #41678451624 passed. The ADR 0006
  clarification is a newer commit and its CI result remains to be checked.
- No runtime, Room schema, protocol, or v1 expiry behavior changed.


## Enrollment clock trust-boundary audit — 2026-11-03

- ADR 0010 documents source-backed clock flow: enrollment services accept caller-supplied `nowEpochMillis`, and current Compose call sites pass the mutable device wall clock.
- The service boundary does not establish that this value is fresh, rollback-resistant, or captured at the actual receipt boundary. This is not evidence of arbitrary user text input at current UI call sites; it is an API trust-boundary limitation.
- Signed event time remains distinct from local receipt/recording time. A plain clock wrapper improves consistency/testability but is not a tamper-resistance mechanism.
- ADR 0006 expiry semantics remain unresolved. No runtime, protocol, or Room schema change was made.
- CI #41680089589 (ADR 0006 clarification) and #41680127933 (Work Log) passed. ADR 0010 commit: `0d7271217f308d7f854555bc7b5871b5628550a8`.


## Finalization replay exactness audit — 2026-11-03

- ADR 0011 documents a confirmed idempotency/result-integrity gap in the owner finalization fast path: current replay matching uses applicant key ID plus signature bytes before validating the complete FMJA1 artifact.
- Existing membership rows do not preserve the exact FMJA1 artifact or its complete-payload digest. A changed envelope retaining the same key ID/signature can therefore be reported as `ALREADY_FINALIZED`; this does not create a second membership row, but does not establish exact replay.
- Proposed remedy is to persist a canonical artifact/digest atomically for new records and define fail-closed or explicit legacy behavior for older rows. No runtime or Room schema change was made.
- ADR 0011 commit: `ede1f826b2a612e32173dc8050b34f7ea6766770`; CI pending.


## Unified Room v7-to-v8 enrollment persistence design — 2026-11-03

- ADR 0012 coordinates two proposed additive schema changes in one v7 → v8 migration: a separate owner-issued decision ledger and a nullable exact-FMJA1 replay digest on authenticated device rows.
- The replay digest is SHA-256 over a domain-separated, canonical re-encoding of the complete FMJA1 envelope, including nested FMOD1 and all applicant acceptance fields. It is an idempotency identifier only; all first-time cryptographic and owner-ledger verification remains mandatory.
- Existing v7 finalized rows must retain NULL replay digest because the complete original FMJA1 artifact cannot be reconstructed. They must not receive exact-replay success based on partial legacy fields.
- ADR 0009 now links the unified migration and adds its exact-replay/legacy-row test requirements.
- No runtime, database schema, protocol, or expiry-policy changes were made. ADR 0008 approval and ADR 0006 policy resolution remain separate gates.
- ADR 0012 commit: `cd1b3316fe14c572ae06f5c8ad3f53d29cd45212`; ADR 0009 coordination commit: `c086128f9f1ee2f0f98197f34d20b1edc375e19d`. CI pending.


## Owner decision issuance consistency audit — 2026-11-03

- ADR 0013 confirms owner FMOD1 issuance is currently transient in Compose and not persisted as an authoritative owner decision.
- The owner can issue distinct opposite decisions for the same pending request. Owner-side FMJA1 finalization verifies the nested decision signature and request binding but does not compare it with a durable owner-issued artifact. A previously signed approval may therefore still finalize while a later rejection has also been issued.
- Applicant-side decision import deletes only the applicant installation's pending row; it does not synchronize the owner installation.
- Required future invariant: one durable authoritative FMOD1 per family/request; exact reissue returns stored artifact; conflicting issuance fails; finalization requires exact match to the stored approval. This is distinct from ADR 0011/0012 exact-FMJA1 replay identity.
- No runtime/schema/protocol/crypto/transport changes. ADR 0008 approval and ADR 0006 expiry policy remain gates.
- ADR 0013 commit: `99df7d830ca6cad0d3c54723de7c262041160b6d`. CI pending.


## Enrollment persistence test-coverage audit — 2026-11-03

- ADR 0014 records that current CI runs `testDebugUnitTest` and `assembleDebug` only; no instrumentation or Room migration suite is executed.
- `:core:storage` currently has no test/androidTest source sets or test dependencies. Room schema export is disabled.
- Existing enrollment service tests use fake repositories. They validate domain/service behavior but do not exercise SQLite transactions, rollback, concurrent uniqueness, foreign keys, migrations, or database reopen durability.
- ADR 0014 proposes staged Room DAO tests, historical migration fixtures, emulator instrumentation, and a separate CI instrumentation job. No test infrastructure or CI changes have yet been made.
- Five CI runs for ADR 0013 and its coordinated docs all succeeded: #41689232031, #41689300728, #41689312246, #41689319946, #41689327795.
- No runtime, Room schema, protocol, crypto, or transport changes. ADR 0008/0012 approval and ADR 0006 expiry-policy resolution remain gates for persistence implementation.


- Follow-up test gap: concurrent applicant imports of the same signed FMOD1 with different local receipt times may return one success and one conflict because the Room entity equality includes observer-only `recordedAtEpochMillis`. ADR 0014 now requires an explicit idempotency decision and deterministic concurrency test; no behavior was changed.


## Initial Room DAO instrumentation baseline — 2026-11-03

- Added `:core:storage` Android instrumentation configuration and a real Room/SQLite test fixture.
- Added DAO tests for nonce replay rejection, applicant decision recording and conflict/replay behavior, and owner finalization and conflict/replay behavior.
- Added a separate emulator-backed CI job that runs `gradle :core:storage:connectedDebugAndroidTest` and uploads reports on success or failure.
- This is an initial baseline, not complete migration validation: historical migration fixtures, injected rollback failures, concurrent same-artifact import races, and close/reopen durability are still outstanding.
- Commits: storage test configuration `3b38a2a1`; CI job `e94f00e9`; DAO tests `5868c4a0`. CI runs #41691001137, #41691009090, #41691021028 were queued/in progress at update time.
- No production runtime, schema, protocol, crypto, or transport behavior changed.


- The initial Room suite has since been expanded with transaction rollback verification (failed pending insert must roll back nonce consumption) and file-backed database close/reopen durability. These tests are awaiting the latest emulator CI result.


## Room instrumentation CI follow-up — 2026-11-04

- Latest main commit at review: `e945b8774d8d43d8426e8f2c2f4f541800d1d2cb`.
- CI run #41697147520: `verify` passed (unit tests, debug APK assembly, artifact upload); `storage-instrumentation` failed before invoking Gradle because API 34 emulator never reported `sys.boot_completed` and the runner ended with `Timeout waiting for emulator to boot`.
- The API 34 attempt used Google APIs / x86_64 / Pixel 2 / SwiftShader, with a 600-second boot timeout. Logs show repeated ADB exit-code-1 polling after the emulator launch. No instrumentation report was produced; this is infrastructure failure, not a test assertion failure.
- Run #41696869594 on the preceding API 34 configuration failed at the same emulator boot boundary. Run #41697076341 for commit `98c1409b` was cancelled when the next main push arrived; this is consistent with a newer run superseding it and is not evidence of a CI test defect.
- Six Room DAO instrumentation tests are now present, including SQLite device-key uniqueness and family foreign-key cascade coverage. None has yet been executed successfully on an emulator.
- No runtime, Room schema, protocol, cryptography, or transport behavior changed. The latest successful verification is limited to the `verify` job; instrumentation remains unverified.


## Room instrumentation and schema export — 2026-11-04

- CI run #41813611838 (run #172), commit `668542632261dac50d98915e76765a5125a4c76f`: both `verify` and `storage-instrumentation` succeeded.
- The emulator executed all six `:core:storage` DAO instrumentation tests: 6/6 completed, zero skipped, zero failed. This is the first successful runtime execution of the six-test suite.
- The previous instrumentation failure was a missing `androidx.test.runner.AndroidJUnitRunner` class. Added the explicit `androidx.test:runner:1.7.0` catalog entry and `:core:storage` androidTest dependency. Commits: `c461e9b24ec5023351bb921547f47a3d76503d87`, `668542632261dac50d98915e76765a5125a4c76f`.
- Room schema export is enabled and KSP generation now runs explicitly with `--rerun-tasks`. Run #41818762325 (run #173), commit `da5e86a94937fcbbc9d0a221b8a2d0c091a50358`, completed the verify job successfully and uploaded `room-schema-snapshot` artifact #13316525198. The instrumentation job was still in progress at this update.
- Inspected the downloaded artifact: it contains `com.familymesh.core.storage.family.FamilyMeshDatabase/7.json`, Room schema version 7, identity hash `3d3bfeb59dfda625d9f6664d1c39765b`. The snapshot lists eight entities, including `authenticated_family_devices`, with its unique composite index on `familySpaceId, deviceKeyId`; family-owned tables declare cascade foreign keys where present.
- This verifies the current v7 exported schema only. It does not create or validate historical schema fixtures for versions 1–6, and it does not prove migration correctness.
- No runtime behavior, Room entities/schema version, protocol, cryptography, expiry policy, or transport semantics were changed. Historical migration fixtures and the approved-design gates remain outstanding.


## Room CI and migration-fixture progress — 2026-11-04

- Run [#41818762325](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41818762325) (#173), commit `da5e86a94937fcbbc9d0a221b8a2d0c091a50358`, completed **both** `verify` and `storage-instrumentation` successfully. The downloaded instrumentation XML reports 6 tests, 0 failures, 0 errors, 0 skipped.
- Run #173 instrumentation artifact #13317910182 confirms all six DAO cases ran on API 34; total test time was 72.646 seconds.
- Run [#41819471027](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41819471027) (#175), commit `b0879b22cf97295109a35c5388c43feb0105a346`, passed `verify` and generated/uploaded schema v7. Its instrumentation attempt timed out while waiting for the emulator to boot; Gradle did not execute. The failed job was re-run independently; the retry is currently in progress.
- The current v7 schema artifact remains a verified snapshot, not a historical migration fixture. The manual historical export workflow for versions 1–7 is committed but has not yet been dispatched.
- No runtime, database schema version, protocol, cryptography, expiry policy, or transport behavior changed.


## Room CI confirmation — run #176 — 2026-11-04

- Run [#41828148184](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41828148184), commit `f2b2988b0a4ffd83784cd3f87b2f2c0fd2a0e082`, completed both `verify` and `storage-instrumentation` successfully.
- Downloaded instrumentation artifact #13325065058 and inspected its JUnit XML directly: suite `EnrollmentNonceDaoInstrumentedTest`, API 34 emulator, 6 tests, 0 failures, 0 errors, 0 skipped; suite time 129.86 seconds.
- All six DAO cases executed, including nonce rollback, database close/reopen durability, authenticated-device uniqueness, and family cascade.
- The earlier emulator boot timeout in run #175 was transient; run #176's complete instrumentation job passed. Historical Room schema fixtures and migration validation for v1–v7 remain outstanding.
- No runtime, Room entity/schema version, protocol, cryptography, expiry policy, or transport behavior changed.


## Historical Room schema artifact audit — 2026-11-04

- Downloaded and parsed all seven historical Room schema JSON artifacts from workflow run #41864521401. Versions 1–7 each contain one matching JSON file; identity hashes and entity sets are recorded in `docs/architecture/room-historical-schema-audit.md`.
- The entity progression is 2→3→4→5→6→7→8 entities, matching the documented migration additions. Columns, affinities, nullability, primary keys, indexes, and foreign keys were inspected. In particular, v5 `trusted_initial_owners` has no FK, matching its historical entity definition.
- The exported schema metadata is consistent with the source migration chain. This does not yet prove SQLite migration execution or row preservation.
- No migration test fixtures have yet been checked into `core/storage/schemas`; no MigrationTestHelper suite has been run. Room remains v7.
- Next: refresh terminal status of all seven export jobs, check in authentic fixtures, add migration tests from versions 1–7, and run them on API 34. Do not implement v8 until the existing ADR approval gates are satisfied.


## Historical Room migration fixture integration — 2026-11-08

- Refreshed Android CI run [#41879166661](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41879166661) on main commit `968b580283b2ce17d0deda83beab4675ec55b845`: the `verify` job passed unit tests, debug APK assembly, and current Room v7 schema export.
- The `storage-instrumentation` job started the API 34 emulator and executed 7 tests. Six completed successfully; the migration test failed before its first migration assertion because `MigrationTestHelper` could not load `com.familymesh.core.storage.family.FamilyMeshDatabase/1.json` from androidTest assets. This is a missing-fixture packaging failure, not a reported migration SQL or row-preservation assertion failure.
- Confirmed all seven historical schema-export matrix jobs in run [#41864521401](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41864521401) completed successfully. Downloaded v1–v7 JSON files parse and declare their matching versions; entity counts progress from 2 to 8. The v7 fixture SHA-256 matches the current main schema snapshot exactly.
- Added all seven historical fixtures to branch `fix/room-migration-schema-fixtures-20261108` under `core/storage/schemas/com.familymesh.core.storage.family.FamilyMeshDatabase/`, which is already configured as the androidTest assets source.
- No runtime, Room entity, schema version, protocol, cryptography, expiry, or transport behavior changed. Migration correctness remains unverified until the fixture-backed API 34 run passes and its JUnit XML is inspected.
