# FamilyMesh Work Log

## 2026-11-27 — Fixed emulator instrumentation retry script for android-emulator-runner

### Observation / Action

- Android CI run #46035651581 on develop/agent-ready failed in the `storage-instrumentation` job with a shell syntax error: `end of file unexpected (expecting "done")`.
- Cause: `reactivecircus/android-emulator-runner@v2` splits multi-line `script:` input and executes each line independently with `sh -c`. The previous multi-line `for` loop was therefore invalid.
- Collapsed the single-retry wrapper into one shell command while preserving the resource limits, `-gpu off`, and extended boot timeout.
- All changes are CI-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Storage instrumentation on API 31 should now execute the retry logic correctly and remain resilient to intermittent system crashes.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Confirm the triggered CI run succeeds.
- Continue protocol-review support without claiming production COSE.
- Keep v8 implementation blocked pending ADR gates.

---

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

(Previous entries preserved in full history; this is the latest engineering log entry.)
