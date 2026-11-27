## 2026-11-27 — Migration instrumentation CASCADE fixed; foreign keys enabled in tests; CI green

- Storage-instrumentation on develop/agent-ready failed because MigrationTestHelper connections leave SQLite foreign_keys OFF by default, so DELETE FROM family_spaces did not cascade (expected 0, got 1).
- Fixed by enabling `PRAGMA foreign_keys = ON` before assertions in FamilyMeshMigrationInstrumentedTest (commit 5bab532b).
- Historical schema fixtures v1–v7 remain checked in. Identity fail-closed unit tests remain expanded.
- Android CI run [#45992543396](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45992543396) (run 262) completed successfully on head `3f7acd73719f601a41d559bf8ef947b85fa0918e`: both `verify` and `storage-instrumentation` passed.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft.
- Room remains v7. Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-27
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Continue protocol-review support (additional KATs, ADR clarity, malformed-input corpus descriptions) without claiming production COSE/E2EE/P2P. Keep v8 implementation blocked pending ADR gates. Confirm any new CI runs remain green.
