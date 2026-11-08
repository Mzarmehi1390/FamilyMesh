# FamilyMesh Handover

Last synchronized: 2026-11-04
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Last verified repository state

- Branch: `main`
- Latest documentation commit: `7112f0ae2bfbb36a37e186b3a9eb4775d3e81213`
- Latest verified source/CI baseline: `f2b2988b0a4ffd83784cd3f87b2f2c0fd2a0e082` (Android CI #176); current HEAD is `7112f0ae2bfbb36a37e186b3a9eb4775d3e81213` after documentation-only updates
- Latest full successful CI: [Android CI #41828148184](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41828148184); run #177 verify passed but instrumentation retry failed before Gradle execution
- CI result: completed successfully; unit tests, debug APK assembly, and artifact upload all passed.
- Branch protection: disabled; no open issues or pull requests were present at the prior synchronization.

## Product and architecture invariants

- Native Android only: Kotlin, Jetpack Compose, Gradle Kotlin DSL.
- Preserve modular architecture: `app`, `core:common`, `core:storage`,
  `core:designsystem`, `core:identity`.
- FamilyMesh remains private, decentralized, and end-to-end encrypted by
  design. Do not replace P2P with a centralized messaging backend.
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

- Commits: `918b03b` (canonical Base64URL), `36e2d27` (P-256 enforcement), `21dd7ca` (parser cleanup).
- CI #41671671596 and #41671944679 passed their respective security-hardening changes.
- CI #41671995919 passed unit tests, APK assembly, and artifact upload for the latest source commit.
- CI #41672460808 completed successfully for the earlier documentation baseline. Later docs commits through ADR 0005 have no status entries exposed by the available status/PR-run queries; CI is unconfirmed.

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
6. Keystore behavior has not been verified by Android API 31+ instrumentation
   tests on physical/emulated devices.
7. Room migrations and transaction behavior need Android instrumentation
   coverage.
8. Gradle Wrapper is absent. CI currently installs Gradle 8.13 and JDK 17.
9. Original Master Engineering Prompt was not present among the accessible
   repository/project files during this synchronization. Do not invent its
   contents; use the repository roadmap and established project invariants.

## Next exact engineering action

Review ADR 0003's COSE v2 profile and produce cross-language known-answer
vectors for the key representation, protected headers, external AAD,
Sig_structure, and ECDSA DER/R||S conversion. Then add parser and malformed
input tests before any migration implementation. Keep networking and
authorization integration blocked until replay scope, trust lifecycle,
revocation, and recovery are also resolved.


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
