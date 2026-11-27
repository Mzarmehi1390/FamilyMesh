# FamilyMesh Work Log

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

## 2026-11-27 — Added COSE v2 primitive KAT 0002 (OwnerDecision context)

### Observation / Action

- Generated and checked in a second reproducible primitive known-answer vector covering the owner-decision artifact context (distinct content type and external AAD from KAT 0001).
- Updated the negative-test outline to apply the rejection checklist to both vectors and to note cross-artifact binding coverage.
- Updated ADR 0003 to reference both primitive fixtures.
- All changes are documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Protocol-review support now has two concrete primitive vectors demonstrating different artifact contexts.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (KATs for remaining contexts, malformed corpus details) without claiming production COSE.
- Keep v8 implementation blocked pending ADR gates.

---

## 2026-11-27 — Confirmed latest green CI on develop/agent-ready (run #268)

### Observation / Action

- Android CI run #46000688306 on develop/agent-ready (head 4603456a6a215835b188093d231c1aacad81ad1e) completed successfully for both `verify` and `storage-instrumentation`.
- This reconfirms the historical schema fixtures and the foreign-key PRAGMA fix remain green.
- Protocol-review support continues via the existing negative-test outline for KAT 0001.

### Impact

- Storage instrumentation and unit tests on this branch remain green.
- No runtime, schema version, protocol, or authorization changes. Room remains v7; ADR 0006/0008/0012 gates unchanged.

### Next

- Continue protocol-review support (additional KATs, ADR clarity, malformed-input descriptions) without claiming production COSE.
- Keep v8 implementation blocked pending ADR gates.

---

(Previous entries preserved in full history; this is the latest engineering log entry.)
