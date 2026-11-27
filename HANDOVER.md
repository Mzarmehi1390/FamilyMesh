# FamilyMesh Handover

Last synchronized: 2026-11-25
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Last verified repository state

- `main` remains unchanged at `968b580283b2ce17d0deda83beab4675ec55b845`; no candidate changes have been merged.
- PR #2 remains open, Draft, and unmerged. Its current branch head is `754e7065eaa414c9689db6f299ba3a9dc30729db` on `build/gradle-wrapper-8.13-20261109`.
- The immediately preceding PR head `997421221cf2ba4e9bcbae4db403620276a79ddf` passed all three jobs in Android CI [run #45556670546](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45556670546).
- `verify` passed the COSE primitive KAT, all 15 backup-policy verifier tests, direct manifest/rules verification, JVM unit tests, aggregate `lintDebug`, Debug APK assembly, Room schema export, and artifact uploads.
- API 31 Keystore instrumentation passed **11/11**; API 34 storage passed **9/9** and identity/Keystore passed **11/11**. All suites had zero failures, errors, or skipped tests; uploaded JUnit XML was inspected.
- The Lint artifact from that run contains 18 warnings in `:app` and zero warnings in each of the four core modules: 16 dependency/toolchain version suggestions, one `OldTargetApi` advisory, and one `ObsoleteSdkInt` warning.
- After that successful run, six reviewed commits were fast-forwarded from `staging/manifest-backup-and-icon-20261125` onto PR #2: remove the redundant adaptive-icon v26 qualifier, update Room from 2.8.4 to 2.8.5, and add JVM plus Android-provider point-at-infinity rejection tests, with supporting log updates.
- The new head is being validated by Android CI [run #45626115545](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45626115545); its final result is not yet known. Do not treat the Room patch, icon-resource move, or point-at-infinity additions as CI-verified until this run completes successfully.
- PR #1 is superseded for integration. The COSE primitive verifier/vector from PR #3 and stable Compose Flow work from PR #4 are present in PR #2; do not merge those older branches independently.
- Branch protection is disabled. Keep PR #2 Draft pending successful validation of the current head, protocol/security review, and owner review.

## Product and architecture invariants

- Native Android only: Kotlin, Jetpack Compose, Gradle Kotlin DSL.
- Preserve modular architecture: `app`, `core:common`, `core:storage`,
  `core:designsystem`, `core:identity`.
- FamilyMesh is intended to become private, decentralized, and end-to-end
  encrypted. E2EE is not implemented yet; do not replace the planned P2P
  architecture with a centralized messaging backend.
- Never claim E2EE, network P2P, BLE, Wi-Fi Direct, relay, or production
  membership authorization exists; those are not implemented.
- Keep all authorization fail-closed until a reviewed trusted membership
  resolver exists.
- Do not silently replace a lost/invalidated device key.

## Completed implementation visible at the latest verified source commit

- Native Android foundation, Compose UI, Koin dependency injection.
- Room family-space and local member metadata persistence; Preferences
  DataStore.
- Android Keystore P-256 ECDSA signing identity and typed lifecycle policy.
- Signed device-enrollment request with nonce and expiry.
- Bounded manual enrollment proof text codec and local nonce replay storage.
- Local initial-owner bootstrap and signed trust bundle.
- Recipient-side owner fingerprint confirmation and immutable local pin.
- Signed owner decision (`FMOD1.`) verified against the pinned owner and
  exact pending request; persisted atomically.
- Applicant-signed acceptance (`FMJA1.`) bound to the exact approved decision.
- Owner-side verification of both signatures and local pending request.
- Room v7 authenticated-device record insertion and pending-request consumption
  in one transaction.
- Home screen lists authenticated devices for the selected family.
- Decision acceptance service resolves the owner key from
  `InitialOwnerPinRepository` internally; UI callers cannot inject an
  arbitrary owner trust record.
- All four enrollment transfer codecs reject non-canonical/padded Base64URL aliases.
- All enrollment signature verification boundaries reject non-P-256 EC public keys.
- Unit tests cover domain, identity, and local persistence behavior.

## Latest code changes and verification

The main-branch implementation baseline is unchanged. The PR #2 integration candidate includes the Gradle 8.13 wrapper with pinned checksums, authentic Room schema fixtures v1–v7 and migration instrumentation, Android Keystore instrumentation on API 31/API 34, COSE primitive KAT validation, Compose Flow lifecycle fixes, P-256 SPKI hardening, backup exclusions, adaptive launcher icon, and aggregate Android Lint.

Android CI [run #45885409892](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45885409892) passed all three jobs on current candidate head `09eaa3832ac50e13b27506953d310adfae4b2f46`. The verify job passed the COSE primitive KAT, 15 backup-policy tests, direct policy check, JVM tests, aggregate Lint, Debug APK assembly, Room schema export, and artifact uploads. API 31 identity/Keystore and API 34 storage/identity instrumentation jobs also passed. The downloaded Lint XML contains 13 app warnings and zero core-module warnings (5 version-availability, 4 dependency, 3 Android Gradle Plugin version, 1 target API advisory).

Emulator results establish behavior on those CI emulator images only. They do not prove hardware-backed Keystore assurance, remote attestation, physical-device compatibility, or security against a compromised operating system. Room remains v7; no v7→v8 migration is implemented or validated. PR #2 remains Draft and unmerged.

## Current work package

**WP — Identity and trusted enrollment protocol review.**

The local enrollment prototype is implemented, but its custom binary payloads
are not the proposed interoperable COSE protocol. ADR 0003 records a
draft v2 profile; primitive KAT 0001 now provides a reproducible signature/encoding fixture.
It remains a proposal: no protocol migration or authorization integration
should be implemented until independent vectors and platform tests validate it.

## Open blockers and risks

1. ADR 0001 remains proposed: code uses JCA P-256 / SHA256withECDSA and
   X.509 public-key bytes with custom domain-separated length-prefixed
   payloads; COSE ESP256 serialization/conversion is not implemented.
2. Nonce consumption is local to one Room database; it is not distributed
   replay protection.
3. Initial-owner trust pinning is a manual out-of-band fingerprint ceremony;
   ownership transfer and multi-owner semantics are undefined.
4. No production `TrustedFamilyMembershipResolver` exists;
   `DenyAllFamilyMembershipResolver` remains the default.
5. No device revocation, recovery, cross-device synchronization, network
   transport, BLE, Wi-Fi Direct, internet P2P, relay, or E2EE messaging.
6. Keystore instrumentation passed on API 31 and API 34 in run #45552834794. Physical-device and hardware-backed assurance/attestation remain unverified.
7. Room v1–v7 migration and DAO instrumentation passed on API 34 in the PR #2 candidate; future v7→v8 behavior remains unverified.
8. The PR #2 candidate adds the official Gradle 8.13 Wrapper with pinned JAR/distribution checksums; main remains unchanged until review.
9. Original Master Engineering Prompt was not present among the accessible
   repository/project files during this synchronization. Do not invent its
   contents; use the repository roadmap and established project invariants.

## Next exact engineering action

Continue the source-backed four-artifact protocol audit. Produce a field/validation parity matrix for the existing request, owner decision, applicant acceptance, and owner finalization envelopes against the draft COSE v2 profile. Explicitly record encoding, key identifiers, signature representation/canonicality, signed timestamps versus local receipt times, expiry, and replay identity. Resolve each unresolved item through the relevant ADR before changing runtime or Room schema. Keep network transport and production authorization blocked until independent interoperability and security review.



## Latest lifecycle audit (documentation proposal)

- ADR 0007 traces the separate applicant and owner Room stores through FMER1,
  FMOD1, and FMJA1. Applicant-side decision import deleting its local pending
  row does not remove the owner's independent request row.
- A concrete owner-inbox gap remains: creating FMOD1 does not persist an
  owner-side decision or update inbox status. Rejected requests remain
  labelled as awaiting review; approved requests remain indistinguishable
  until the owner receives FMJA1 and finalizes.
- Pending-request expiry cleanup is still absent. Do not delete owner pending
  rows on approval issuance until a reviewed design preserves the request
  facts required by FMJA1 verification.
- ADR 0007 is an audit/design proposal only. No runtime or schema change was
  made. Required next work: decide owner-side lifecycle states, then implement
  cross-device state tests and Room instrumentation tests.


## 2026-11-03 — Room and expiry review handover

- ADR 0009 records the current Room v7 migration chain and the fact that
  current CI does not run Android instrumentation tests. It defines the
  migration and transaction test acceptance criteria for a future owner
  decision ledger; it does not change schema or runtime.
- ADR 0006 now compares receipt-deadline and signed-event-deadline expiry
  policies. No policy or numeric TTL/skew value has been selected.
- Before implementing owner-ledger persistence, obtain product/security
  answers to the offline-delivery, expiry-purpose, delivery-delay,
  clock-rollback, and retention questions in ADR 0006.
- Keep schema changes additive, preserve applicant-side
  `enrollment_decisions` semantics, and require exact owner-issued FMOD1
  matching before FMJA1 finalization.
- CI #41678430461 (ADR 0009) and #41678451624 (Work Log) passed. Verify CI
  independently for the later ADR 0006 clarification commit.


## 2026-11-03 — Enrollment clock trust-boundary handover

- ADR 0010 traces `nowEpochMillis` through proof receipt, decision import, and owner finalization services to the Compose call sites. Current UI supplies `System.currentTimeMillis().coerceAtLeast(0L)`; services do not independently authenticate or acquire time.
- Treat this as a mutable wall-clock/caller-boundary limitation, not proof that current UI accepts arbitrary timestamp text. A clock abstraction alone is not a security-grade anti-rollback source.
- Keep signed event timestamps separate from local receipt/recording timestamps.
- Do not change expiry/finalization behavior or add cleanup until ADR 0006's policy and threat model are accepted. Then review ADR 0008/0009 before implementing owner ledger/schema work.
- ADR 0006 clarification CI #41680089589 and Work Log CI #41680127933 passed. ADR 0010: `0d7271217f308d7f854555bc7b5871b5628550a8`.


## 2026-11-03 — Finalization replay audit handover

- ADR 0011 records that the current owner-side already-finalized path compares applicant key ID and signature bytes before validating the complete FMJA1 artifact. This can report a changed envelope as an already-finalized replay if those two values are retained.
- Existing membership rows lack an exact FMJA1 artifact or complete-payload digest, so do not claim exact replay for legacy records without an explicit compatibility policy.
- A future additive replay-identity field must be persisted atomically with membership finalization and covered by Room migration, legacy-row, restart, and concurrency tests. Coordinate with ADR 0008/0009.
- No runtime/schema change was made. ADR 0011 commit: `ede1f826b2a612e32173dc8050b34f7ea6766770`; CI pending.


## 2026-11-03 — Unified Room v7-to-v8 persistence handover

- ADR 0012 proposes one additive Room 7 → 8 migration containing both the ADR 0008 owner-issued decision ledger and exact FMJA1 replay digest. Do not create two competing migrations from v7.
- Digest input is a versioned domain separator ending in NUL plus UTF-8 of `EnrollmentAcceptanceTextCodec.encode(decodedAcceptance)`. The canonical codec includes nested FMOD1 and all acceptance fields. Digest is an idempotency identifier, never a replacement for first-time signature/trust verification.
- v7 authenticated-device rows migrate with NULL digest. Their original FMJA1 artifact is not recoverable from stored columns; exact-replay success must not be inferred for these legacy rows.
- ADR 0009 links the unified migration and includes replay identity coverage. No runtime or schema code changed.
- Before implementation: approve ADR 0008 ledger semantics; review ADR 0012; keep ADR 0006 expiry decisions separate; add emulator/Room migration coverage as required by ADR 0009.
- ADR 0012: `cd1b3316fe14c572ae06f5c8ad3f53d29cd45212`; ADR 0009 update: `c086128f9f1ee2f0f98197f34d20b1edc375e19d`. CI pending.


## 2026-11-03 — Owner decision issuance consistency handover

- ADR 0013: owner approve/reject actions currently sign FMOD1 into transient Compose state; they do not persist the issued decision. The owner can sign opposite outcomes for the same pending request.
- Applicant-side import deletes only the applicant-local pending row. It does not update the owner installation.
- Owner finalization validates the nested FMOD1 signature/request binding but does not compare it with a durable owner-issued artifact. Therefore an earlier signed approval may still finalize after a later rejection was also issued, while the owner pending row remains.
- Required invariant: persist one authoritative exact FMOD1 per family/request; exact reissue returns stored text; a different artifact/outcome conflicts; finalization requires exact match to the stored approval. Do not solve with a UI-only flag or by deleting request facts.
- This is distinct from FMJA1 replay identity in ADR 0011/0012. Coordinate both in the single v7 → v8 migration proposal. Keep ADR 0006 expiry decisions separate.
- No runtime/schema/protocol/crypto/transport changes. ADR 0013: `99df7d830ca6cad0d3c54723de7c262041160b6d`; CI pending.


## 2026-11-03 — Enrollment persistence test-coverage handover

- ADR 0014 audits the actual validation boundary: current CI runs unit tests and debug APK assembly, not instrumentation or Room migration tests.
- `:core:storage` currently has no test/androidTest source sets or test dependencies; Room schema export is disabled.
- The enrollment identity service tests use fake repositories. Do not treat them as evidence for SQLite transaction rollback, concurrent uniqueness, foreign-key behavior, migrations, or database reopen durability.
- ADR 0014 defines the required sequence: add Room DAO tests, build historical schema fixtures for versions 1–7, test migrations/constraints, then run emulator instrumentation as a separate CI check.
- Five CI runs for ADR 0013 and its coordinated documentation updates succeeded (#41689232031, #41689300728, #41689312246, #41689319946, #41689327795).
- ADR 0014 is documentation-only. No runtime, schema, CI, protocol, crypto, or transport changes were made. Before implementing owner-issued persistence, review ADR 0008/0012 and resolve ADR 0006 expiry policy.


- Follow-up: ADR 0014 records a potential same-artifact import race. Sequential replay ignores local `recordedAtEpochMillis`, but the DAO's concurrent existing-row equality includes it; test same signed FMOD1 arriving concurrently with different local receipt times before deciding whether that race should be idempotent or conflict.


## 2026-11-03 — Room DAO instrumentation baseline handover

- `:core:storage` now has an Android instrumentation runner and an initial Room/SQLite test class covering nonce replay rejection, applicant decision persistence/replay/conflict, and owner finalization/replay/conflict.
- CI has a separate emulator-backed `storage-instrumentation` job invoking `gradle :core:storage:connectedDebugAndroidTest`; reports are uploaded even on failure.
- This does not yet satisfy all ADR 0009/0014 acceptance criteria. Still required: real historical schema fixtures for versions 1–7, injected rollback failures, concurrent import/issuance cases, and database close/reopen durability.
- Initial commits: `3b38a2a1` (storage test configuration), `e94f00e9` (CI), `5868c4a0` (DAO tests). Verify the first emulator CI results before extending the suite.
- No production runtime, schema, protocol, cryptographic, or transport changes.


- The instrumentation suite was expanded after the baseline: it now checks rollback of nonce insertion when pending insertion fails and verifies pending/nonce state after closing and reopening a file-backed database. Verify the latest emulator run before treating these cases as validated.


## 2026-11-04 — Historical Room schema artifacts inspected

- Downloaded and parsed all seven v1–v7 schema JSON artifacts from workflow run #41864521401. Full inventory, identity hashes, entity progression, indices, FKs, and limitations are in `docs/architecture/room-historical-schema-audit.md`.
- Exported schema shapes align with the source migration chain. This is not yet a MigrationTestHelper or SQLite upgrade pass; no row-preservation result is claimed.
- Current Room version remains 7. No v8 schema/runtime change was made.
- Latest repository HEAD: `7112f0ae2bfbb36a37e186b3a9eb4775d3e81213`.
- Next exact action: confirm all seven workflow job conclusions, add the authentic schema fixtures under the Room schema directory, implement MigrationTestHelper coverage for starting versions 1–7, then execute on API 34 and inspect JUnit XML.


## 2026-11-08 — Historical Room migration fixtures handover

- Main HEAD reviewed: `968b580283b2ce17d0deda83beab4675ec55b845`.
- Android CI run [#41879166661](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41879166661): `verify` passed; `storage-instrumentation` executed seven tests, with six passing and the migration test failing before migration assertions because the v1 schema JSON was not packaged in androidTest assets.
- All seven historical export jobs from [run #41864521401](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41864521401) succeeded. The v1–v7 JSON fixtures were parsed, version/entity counts checked, and v7 was confirmed byte-identical to the current generated schema.
- Branch `fix/room-migration-schema-fixtures-20261108` now includes the seven fixtures at the configured Room schema asset path. No production schema/runtime behavior was changed.
- Next exact action: let the branch CI run, inspect the API 34 JUnit XML, and only then mark migration validation complete. If it passes, continue with the separately recorded DAO same-artifact concurrency/idempotency test. Keep v7→v8 owner-ledger changes blocked on ADR 0006/0008/0012 review.


## 2026-11-13 — Room migration validation completed; DAO race coverage next

- PR #2 combined branch head: `2897397fa2e9f672f891c2f2f61f26ada901a372`.
- CI [run #43231837501](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/43231837501) completed both jobs successfully.
- Downloaded and inspected JUnit artifact #13952322275: 7 tests, 0 failures, 0 errors, 0 skipped on API 34.
- The migration test passed for all supported start versions 1–7, with representative row preservation, expected tables, indexes, unique device-key enforcement, and family FK cascades. Room remains v7.
- Gradle 8.13 wrapper, unit tests, Debug APK assembly, and schema export passed.
- The seven historical schema fixtures and wrapper are integrated on PR #2; no changes were made to main.
- Next: execute deterministic DAO concurrency tests for same signed FMOD1 with distinct local receipt times and for opposite artifacts. The current same-artifact DAO equality behavior remains a characterization, not an approved idempotency policy. Keep v7→v8 owner-ledger/replay changes blocked on ADR 0006/0008/0012 review.


## 2026-11-18 — PR #2 integration candidate validation refresh

- The default branch remains unchanged at `968b580283b2ce17d0deda83beab4675ec55b845`; this work is still isolated on `build/gradle-wrapper-8.13-20261109` and has not been merged.
- PR #2 candidate head: `dc16231b10276443b5169876c4af2ff1232c824e`.
- Android CI run [#43602180283](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/43602180283), attempt 4, completed successfully on 2026-11-17. All three jobs passed: `verify`, API 34 `storage-instrumentation`, and API 31 `identity-api31-instrumentation`.
- The verify job passed JVM unit tests, Debug APK assembly, Room schema generation, and artifact uploads. API 34 instrumentation passed the Room v1–v7 migration suite, DAO concurrency characterizations, and Keystore tests; API 31 Keystore instrumentation also passed.
- Room remains schema v7. The historical migration result validates supported v1–v7 paths only; it is not evidence for a future v7→v8 migration.
- The same-artifact/different-receipt-time and opposite-decision DAO tests are characterization tests. They do not approve idempotency, conflict, expiry, or owner-decision product semantics.
- PR #1 remains superseded for integration by PR #2 and must not be merged independently.
- No production runtime, Room entity/schema version, protocol, cryptographic behavior, expiry policy, or network transport changed in this validation stage.

### Next work and safety gates

Continue the identity/trusted-enrollment review without enabling messaging transport or production authorization. The unresolved gates remain: independent COSE/CBOR vectors for all four artifacts; deterministic encoding and ECDSA low-S policy; v1/v2 key-ID and serialization migration; expiry and replay-retention semantics; trusted owner ceremony, ownership transfer, revocation, recovery; and a threat model covering clock rollback, local database tampering, backup/restore, and offline peers. Do not implement the owner decision ledger or a v7→v8 migration until ADR 0006/0008/0012 semantics are reviewed and approved.


## 2026-11-21 — Compose observation stability and primitive KAT integration

- Ported PR #4's memoized observation Flows into the existing PR #2 validation candidate. Repository-backed Flow instances are remembered by repository/service identity; authenticated devices are additionally keyed by selected family-space ID.
- Ported PR #3's fixture-only Python/OpenSSL verifier and wired it into PR #2's existing wrapper-based CI. The verifier reproduces KAT 0001 bytes and checks the signature plus AAD/payload tampering; it is not a production COSE parser or protocol approval.
- PR #3 and PR #4 standalone verify jobs passed, while their API 34 instrumentation jobs did not reach Gradle tests because the hosted runner disabled hardware acceleration and the emulator/ADB device failed to start. Treat those runs as infrastructure failures, not test assertion failures.
- The previous PR #2 head passed all three jobs in run #44317110977. Await CI for the newly extended PR #2 head before marking this integration complete.


## 2026-11-23 — Hosted Linux emulator startup root-cause fix

### Investigation

- Inspected PR #2 head `d7ebfd44ae9d5bf81b5ed02e164764947759fb6a` and Android CI run [#45114550680](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45114550680).
- The `verify` job passed the COSE primitive fixture verifier, JVM unit tests, Debug APK assembly, Room schema generation, and both artifact uploads.
- Both API 31 and API 34 instrumentation jobs failed before Gradle instrumentation started. Raw job logs explicitly reported: `Software CPU emulation (-accel off) is not supported. A hardware CPU accelerator is required (or set ANDROID_I_WANT_MY_TCG=yes to force TCG software emulation).` The runner also reported that hardware acceleration was unavailable.
- This is an emulator-host capability/configuration failure, not a test assertion failure. No instrumentation result is claimed for run #45114550680.

### Change

- Set job-level `ANDROID_I_WANT_MY_TCG: "yes"` in both hosted-Linux emulator jobs so QEMU TCG software CPU emulation is explicitly permitted when KVM/hardware acceleration is unavailable.
- Kept the API levels, emulator image, test commands, timeout, and test coverage unchanged.
- Workflow fix commit: `78f68e1dff252d9e9865d6a25606252e7a9c19e7`.

### Validation boundary

- The workflow edit is committed to PR #2's existing integration branch. A new CI run must confirm whether TCG can boot these images and whether instrumentation tests execute. Until that run completes, emulator validation remains pending.
- No Kotlin runtime, database schema, protocol, cryptography, authorization, or transport behavior changed. No performance gain is claimed.


## 2026-11-23 — Remove duplicate cancellation handlers in Home screen

### Audit finding

- Re-read the complete 921-line `FamilyMeshHomeScreen.kt` on the active integration branch after the prior cancellation-safety change.
- Found four consecutive duplicate `catch (cancelled: CancellationException) { throw cancelled }` clauses in owner approval, decision import, applicant acceptance signing, and owner finalization actions.
- These duplicate clauses added no behavior and obscured the cancellation/error boundary.

### Change

- Removed exactly four redundant cancellation handlers while retaining one cancellation rethrow before each ordinary `Exception` handler.
- Confirmed by source-level audit that no duplicate adjacent cancellation-handler pairs remain and that no `runCatching` calls remain in this screen.
- Commit: `595b189e5707f26666d14fa6886e9f0b010be2ed`.

### Validation boundary

- No local Android build is available in this session. The updated PR CI must compile and test this cleanup.
- This is a behavior-preserving cleanup; no performance gain is claimed. No domain, Room schema, protocol, cryptographic, authorization, or transport behavior changed.


## 2026-11-24 — Current integration and CI handover

- Current integration branch: `build/gradle-wrapper-8.13-20261109`; baseline head before the latest workflow update: `90de4eaaa6b7505dd3c06e0c43c8d755c9437803`.
- PR #2 is open, Draft, and not merged. PR #1 remains superseded; PRs #3 and #4 contain changes already integrated into PR #2 and must not be merged independently.
- Latest completed CI before this update: [run #45191763315](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45191763315). The `verify` job passed. API 34 did not complete emulator boot; API 31 booted but failed at the action's ADB unlock step. Neither instrumentation Gradle command ran.
- The workflow now prepares/caches AOSP API 31 and API 34 emulator snapshots, explicitly starts ADB before launch, uses `swiftshader_indirect`, and allows 1,200 seconds for cold boot. Wait for the next PR CI result before treating these jobs as verified.
- No source runtime, Room schema, cryptographic profile, authorization, or transport change was made in this cycle. Room remains v7 and the draft COSE primitive verifier remains fixture-only.
- Next priorities: verify the new emulator workflow; then resume the ADR-gated protocol review (four-artifact parity, independent deterministic-CBOR/COSE evidence, time/replay semantics, owner-decision authority, revocation and recovery). Do not start production messaging, P2P, BLE, Wi-Fi Direct, relay, or schema v8 before those decisions are approved.


## 2026-11-24 — P-256 parser hardening staged

A source audit identified that the common P-256 SPKI parser checked curve parameters but did not itself assert coordinate bounds and the curve equation. A candidate adds those checks and two malformed-point JVM tests. It is staged on `staging/security-and-ci-hardening-20261124`, based on PR #2 head `93adcffc67d75d70d8b9397b1cd9f7308b169160`, and is not yet part of PR #2. First allow run [#45331772915](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45331772915) to finish; then fast-forward the PR branch to the staged candidate and require the next CI run to pass. This change does not implement COSE v2 or alter the protocol.


## 2026-11-24 — Emulator workflow redesign staged

The current PR run proves API 31 can boot and pass 8/8 Keystore tests, but its second emulator launch failed to restore `default_boot` (Error -22) and fell back to cold boot. A candidate revision on `staging/security-and-ci-hardening-20261124` removes the separate warm-up boot, caches AVD definition plus AOSP system image, disables snapshots explicitly, and runs each API's tests in one emulator action. Both workflows also move to Node 24-native action versions. The candidate is not yet in PR #2; wait for run #45331772915 to finish before fast-forwarding it.


## 2026-11-24 — PR #2 integration CI fully green

- Android CI run [#45331772915](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45331772915) completed successfully on PR #2 head `93adcffc67d75d70d8b9397b1cd9f7308b169160`; all three jobs passed.
- `verify` passed the draft COSE primitive KAT verifier, `testDebugUnitTest`, `assembleDebug`, Room schema generation, and artifact uploads.
- API 31 Keystore instrumentation passed 8/8 tests with 0 failures and 0 skipped.
- API 34 instrumentation passed 9/9 storage tests and 8/8 identity tests, with 0 failures and 0 skipped in both suites.
- The prior two-action emulator setup still logged a Quick Boot restore `Error -22` on API 31 and fell back to a cold boot; this did not prevent the test suite from executing and passing. The new single-boot AVD/system-image cache redesign is intended to remove that redundant snapshot path, but remains unverified until a fresh run.
- The two commits on `staging/security-and-ci-hardening-20261124` add explicit P-256 imported-point validation with negative JVM tests and redesign emulator caching/action versions. They are not included in PR #2 yet and are not covered by run #45331772915.
- Next action: fast-forward the PR #2 working branch to the staged candidate and require a new full CI run. Keep PR #2 Draft and unmerged; the COSE v2, owner-decision, expiry, replay, revocation, and recovery reviews remain open.


## 2026-11-24 — Single-boot emulator workflow and P-256 hardening verified

- Current PR #2 head: `79304843f42ad1ef1fc0e331c2f562a2da731302`; PR #2 remains open, Draft, and unmerged. Default branch `main` remains unchanged at `968b580283b2ce17d0deda83beab4675ec55b845`.
- Android CI run [#45406912355](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45406912355) completed successfully across all three jobs on this exact head.
- `verify`: the draft COSE primitive KAT verifier, `testDebugUnitTest`, `assembleDebug`, Room schema generation, and artifact uploads succeeded.
- API 34: Room/storage instrumentation 9/9 and identity/Keystore instrumentation 8/8 passed; zero failures and zero skipped.
- API 31: identity/Keystore instrumentation 8/8 passed; zero failures and zero skipped.
- The new single-boot AVD + AOSP system-image cache workflow executed both emulator jobs successfully. Initial ADB polling emitted transient exit-code-1 messages and emulator-console warnings, but both Gradle instrumentation commands subsequently ran and passed. This run confirms test execution, not a measured boot-time or cache-efficiency improvement.
- The common v1 P-256 SPKI import boundary now rejects point-at-infinity, out-of-field coordinates, and off-curve points; its malformed-point JVM tests are included in the passing verify job. This does not implement or approve COSE v2.
- Artifacts include the Debug APK, Room schema snapshot, and API 31/API 34 instrumentation reports. Emulator tests do not prove hardware-backed Keystore assurance, attestation, or physical-device compatibility.
- Next: continue the ADR-gated enrollment protocol review. Resolve the COSE v2 schema/interop, DER-to-R||S and low-S policy, key-ID migration, time/replay semantics, owner-issued decision ledger, exact FMJA1 replay identity, and trust/revocation/recovery design before changing Room v7, authorization, or any network transport. Keep PR #2 Draft and do not merge it automatically.
