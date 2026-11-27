## 2026-11-27 — Migration instrumentation CASCADE fixed; foreign keys enabled in tests

- Storage-instrumentation on develop/agent-ready failed because MigrationTestHelper connections leave SQLite foreign_keys OFF by default, so DELETE FROM family_spaces did not cascade (expected 0, got 1).
- Fixed by enabling `PRAGMA foreign_keys = ON` before assertions in FamilyMeshMigrationInstrumentedTest (commit 5bab532b).
- Historical schema fixtures v1–v7 remain checked in. Identity fail-closed unit tests remain expanded.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft.
- Room remains v7. Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-27
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Confirm the next Android CI run on `develop/agent-ready` is green for both `verify` and `storage-instrumentation`. Continue protocol-review support (additional KATs, ADR clarity) without claiming production COSE/E2EE/P2P. Keep v8 implementation blocked pending ADR gates.
