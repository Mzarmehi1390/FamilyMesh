# FamilyMesh Work Log

## 2026-11-27 — Fixed missing foreign-key enforcement in migration instrumentation

### Observation / Action

- Android CI run #45984312178 on develop/agent-ready failed in storage-instrumentation: FamilyMeshMigrationInstrumentedTest asserted expected 0 but was 1 after DELETE FROM family_spaces.
- Root cause: MigrationTestHelper opens raw SQLite connections with foreign_keys OFF, so ON DELETE CASCADE never fired.
- Added `migrated.execSQL("PRAGMA foreign_keys = ON")` before assertions (commit 5bab532b). No other changes.

### Impact

- Cascade and constraint checks now match production Room behavior on this branch.
- No runtime, schema version, protocol, or authorization changes. Room remains v7; ADR 0006/0008/0012 gates unchanged.

### Next

- Confirm the next Android CI run on develop/agent-ready is green for both verify and storage-instrumentation.
- Continue protocol-review support within existing gates.

---

## 2026-11-27 — Expanded identity fail-closed unit tests on develop/agent-ready

### Observation / Action

- Expanded CanonicalBase64UrlTest and P256PublicKeyParserTest with additional rejection cases (empty/whitespace, plus-sign alphabet, truncated SPKI, non-EC algorithm identifiers, malformed bytes) — commit 3d70f74f.
- These strengthen the fail-closed boundaries of the existing enrollment codecs/verifiers without changing runtime behavior.

### Impact

- Broader unit-test surface for security-sensitive parsers.
- No schema, protocol, authorization, or production-claim changes. Room remains v7.

### Next

- Confirm the next Android CI run on develop/agent-ready is green for both verify and storage-instrumentation (fixtures already present).
- Continue protocol-review support within existing gates.

---

(Previous entries preserved in full history; this is the latest engineering log entry.)
