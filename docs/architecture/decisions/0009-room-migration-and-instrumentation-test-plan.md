# ADR 0009 — Room migration and instrumentation test strategy

- Status: TEST PLAN — proposed; no schema or runtime changes approved
- Date: 2026-11-03
- Depends on: ADR 0007 and ADR 0008

## Context verified in source

The current `FamilyMeshDatabase` is Room schema version 7. Its registered
migration chain is:

- 1 → 2: consumed enrollment nonces;
- 2 → 3: pending enrollment requests and their indexes;
- 3 → 4: initial owner trust;
- 4 → 5: trusted initial owners;
- 5 → 6: applicant-side enrollment decisions and their indexes;
- 6 → 7: authenticated family devices and a unique
  (familySpaceId, deviceKeyId) index.

The current CI workflow runs `gradle testDebugUnitTest` and
`gradle assembleDebug`, then uploads the APK. It does not run
`connectedAndroidTest` or another instrumentation task. The app declares an
instrumentation runner and some Android test dependencies, but
`:core:storage` does not currently declare a Room migration-test setup.
Room schema export is disabled (`exportSchema = false`).

These observations mean a green current CI run is not evidence that an
existing user's on-device database can migrate safely or that multi-step
Room transactions behave correctly on SQLite.

## Goals

1. Preserve user data across every supported database upgrade.
2. Test real SQLite/Room behavior for atomicity, uniqueness, foreign keys,
   and rollback rather than relying only on fakes.
3. Make future schema changes reviewable and reproducible in CI.
4. Keep applicant-side decision records separate from the proposed
   owner-issued decision ledger in ADR 0008.
5. Avoid silently treating a successful APK build as migration validation.

## Migration requirements for the ADR 0008 implementation

If the owner-issued decision ledger is approved, the implementation should
increment the schema from 7 to 8 and add exactly one forward migration
7 → 8. It must not rewrite or repurpose the existing
`enrollment_decisions` table.

The new table should:

- use the composite primary key (familySpaceId, requestId);
- retain the exact canonical FMOD1 text required by the current FMJA1
  binding semantics;
- store applicant and owner key IDs, outcome, signed decision time, and
  local recording time;
- reference `family_spaces(id)` with the existing cascade-delete policy;
- define indexes only for demonstrated query paths (for example inbox
  ordering or outcome filtering), avoiding redundant indexes.

The migration must be additive. It must not fabricate owner decisions for
legacy pending requests, infer that a transient UI artifact was issued, or
delete request facts needed by FMJA1 verification. Legacy rows without an
owner-issued ledger entry must continue to fail closed at finalization.

No `EXPIRED` status or expiry cleanup should be introduced by this migration
until ADR 0006's receipt-time versus signed-event-time policy is explicitly
resolved. Schema migration and expiry-policy changes should be separate,
reviewable changes.

## Required instrumentation coverage

### Migration compatibility

- Build representative database fixtures for every supported starting
  version (1 through 7), using the actual historical table and index shapes.
- Open each fixture with the current Room database and verify the migration
  reaches version 7 (or version 8 after the ledger is approved).
- Assert preservation of representative rows in family spaces, members,
  nonces, pending requests, trust records, applicant decisions, and
  authenticated devices wherever those tables exist in the source version.
- Assert primary keys, foreign keys, cascade behavior, and unique indexes
  after migration.
- Verify a fresh install creates the latest schema without running upgrade
  migrations.
- Do not use destructive fallback in production to make a migration test pass.

### Owner decision ledger and finalization

- Approval and rejection each persist the exact FMOD1 text before success is
  reported to the UI.
- Repeating an identical issuance returns the stored artifact; a different
  artifact for the same request conflicts without overwriting it.
- A storage exception leaves no partial ledger or inbox transition.
- A rejected request can never produce an authenticated membership.
- FMJA1 with the exact locally issued approval finalizes once.
- Missing, rejected, or mismatched owner ledger records fail closed.
- Finalization replay is idempotent; conflicting device, key, or role data
  conflicts.
- Inject failures between writes and verify the Room transaction rolls back
  ledger, request lifecycle, and membership changes as one unit.
- Exercise two concurrent issuance/finalization attempts against the same
  request and assert one deterministic committed result.
- Verify process/database reopen preserves the issued artifact and derived
  inbox state.
- Test applicant and owner repositories as separate database instances so
  applicant-side import cannot mutate owner-side records.

### Expiry boundary

Until ADR 0006 is decided, tests should document both candidate policies as
pending acceptance criteria rather than encode either as production
behavior. Once selected, add exact-boundary tests for now-before-created,
now-equals-created, now-before-expiry, now-equals-expiry, delayed delivery,
and cleanup. Signed event time and local receipt time must remain distinct.

## CI plan

Add an Android instrumentation job that runs the storage migration and
transaction suite on a pinned emulator/API image. Keep the existing fast
unit-test and APK-assembly job. The instrumentation job should:

- run on pull requests and pushes to main;
- publish test reports and emulator/logcat diagnostics on failure;
- use deterministic fixtures and avoid network dependencies;
- fail the workflow on any migration or transaction test failure.

A separate build-only CI result must never be described as having passed
instrumentation or migration tests.

## Acceptance gates

- ADR 0008's owner-ledger semantics and ADR 0006's expiry policy are reviewed.
- Migration 7 → 8 is additive and preserves all existing data.
- Every supported historical migration path is exercised on SQLite.
- Transaction, conflict, replay, and rollback tests pass on an emulator.
- CI visibly reports the instrumentation job independently from unit tests
  and APK assembly.

## Non-goals

This ADR does not implement the ledger, change the database schema, select
the expiry policy, change the v1 wire format or cryptography, introduce
distributed synchronization, or claim that local Room state is a
distributed source of truth.
