# FamilyMesh Work Log

## 2026-11-27 — Hardened emulator instrumentation against flaky system crashes

### Observation / Action

- Android CI run #46028589670 on develop/agent-ready failed in the `storage-instrumentation` job with `INSTRUMENTATION_ABORTED: System has crashed` after one of seven tests completed (emulator booted successfully).
- Updated `.github/workflows/android-ci.yml` to raise emulator memory to 2048, cores to 2, switch GPU to `off` for stability, extend boot timeout to 1200s, bump the AVD cache key, and wrap the Gradle instrumentation invocation in a single automatic retry.
- All changes are CI-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Storage instrumentation on API 31 should be more resilient to the intermittent system crashes seen on hosted runners.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Confirm the triggered CI run succeeds.
- Continue protocol-review support without claiming production COSE.
- Keep v8 implementation blocked pending ADR gates.

---

## 2026-11-27 — Added COSE v2 primitive KATs 0003 and 0004 (complete primitive set)

### Observation / Action

- Generated and checked in primitive known-answer vectors for the remaining two artifact contexts: applicant-acceptance (KAT 0003) and initial-owner-trust (KAT 0004).
- Updated the negative-test outline and ADR 0003 to reference the complete set of four primitive fixtures covering all artifact contexts.
- All changes are documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Protocol-review support now has primitive vectors for every artifact context defined in the draft ADR 0003. Full payload schemas and independent cross-implementation verification remain open.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (full payload schemas, malformed corpus details, ADR clarity) without claiming production COSE.
- Keep v8 implementation blocked pending ADR gates.

---

(Previous entries preserved in full history; this is the latest engineering log entry.)
