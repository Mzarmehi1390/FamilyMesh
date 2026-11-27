# FamilyMesh Work Log

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

## 2026-11-27 — Historical Room schema fixtures checked in on develop/agent-ready

### Observation / Action

- Android CI run #45975678330 on develop/agent-ready failed in storage-instrumentation because FamilyMeshMigrationInstrumentedTest could not find schema files (FileNotFoundException for com.familymesh.core.storage.family.FamilyMeshDatabase/1.json).
- Checked in the authentic historical schema JSON artifacts for versions 1–7 (commit fcacf136). These match the previously audited exports already present on PR #6 and the integration candidate.
- PR #2 remains Draft with full green CI on head f47634412 (run #45943777424).

### Impact

- MigrationTestHelper coverage can now execute on this branch.
- No runtime, schema version, protocol, or authorization changes. Room remains v7; ADR 0006/0008/0012 gates unchanged.

### Next

- Confirm the next Android CI run on develop/agent-ready is green for both verify and storage-instrumentation.
- Continue protocol-review support work within existing gates.

---

(Previous entries preserved in full history; this is the latest engineering log entry.)
