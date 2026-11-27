# FamilyMesh Work Log

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

## 2026-11-27 — API 31 emulator stabilization applied to develop/agent-ready

### Observation / Action

- Observed repeated `Timeout waiting for emulator to boot` on API 34 storage-instrumentation jobs (e.g. Android CI run #45967740726 on develop/agent-ready).
- Updated `.github/workflows/android-ci.yml` on `develop/agent-ready` (commit `dec517de`) to use API 31 (minSdk), default target, AVD/system-image caching, longer timeout, and snapshot-disable.
- Feature branch `ci/stabilize-emulator-and-expand-negative-tests-20261127` already contains the same CI change plus expanded CanonicalBase64Url / P256PublicKeyParser unit tests (commit `8a2956a2`).
- PR #2 integration candidate confirmed full green CI on head `f47634412` (run #45943777424).

### Impact

- Infrastructure reliability improvement for documentation-branch CI (next run should be monitored).
- Broader fail-closed test surface available on the dedicated feature branch.
- No schema, protocol, authorization, or production-claim changes.

### Next

- Confirm the next Android CI run on develop/agent-ready is green for both jobs.
- Open PR for the CI + negative-test branch once interactive confirmation is available, and continue KAT / ADR clarity work within existing gates.

---

(Previous entries preserved in full history; this is the latest engineering log entry.)
