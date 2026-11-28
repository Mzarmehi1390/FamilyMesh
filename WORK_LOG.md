# FamilyMesh Work Log

## 2026-11-28 — COSE v2 payload-schema proposal and expanded negative-test mutations

### Observation / Action

- Protocol review still needed concrete schema candidates and additional mutation examples.
- Added a review-only payload schema proposal mapping ADR 0005 inventory to candidate CBOR labels, types, and byte limits for the four artifacts (exact-decision binding preferred; identity-creation time remains transfer metadata).
- Expanded the negative-test outline with concrete protected-header bit-flip and signature-length examples from KAT 0001 and linked the schema proposal.
- Documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Reviewers now have an explicit candidate schema and more actionable mutation illustrations without any claim of parser implementation or profile approval.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (refine schema with feedback, complete positive vectors once labels discussed, additional corpus details).
- Keep v8 implementation blocked pending ADR gates.

---

## 2026-11-28 — Expanded COSE v2 negative-test outline with concrete mutation examples

### Observation / Action

- The existing negative-test outline provided a high-level checklist derived from the four primitive KATs.
- Added a new section with concrete byte-level mutation examples drawn from KAT 0001 (payload bit-flip on the minimal CBOR, AAD substitution with the KAT 0002 value, trailing-bytes note, and alg-substitution guidance).
- Documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Reviewers have more actionable illustrations of expected fail-closed behavior without any claim of parser implementation or profile approval.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (full payload schemas, additional corpus details).
- Keep v8 implementation blocked pending ADR gates.

---

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

(Previous entries preserved in full history; this is the latest engineering log entry.)
