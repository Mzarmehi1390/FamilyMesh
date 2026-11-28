# FamilyMesh Work Log

## 2026-11-28 — Concrete high-S values and payload size analysis for COSE v2 review

### Observation / Action

- Low-S conversion notes previously described high-S construction only abstractly; payload schema proposal lacked size estimates and an encoding sketch.
- Added concrete `n - s` high-S complements for KAT 0001 and KAT 0002, a matching concrete high-S example in the negative-test outline, approximate maximum encoded sizes under deterministic CBOR, and a toy enrollment-proof payload encoding sketch.
- Documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Reviewers have pasteable high-S values and a size/encoding aid for discussing payload schemas without any claim of parser implementation or profile approval.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (refine schema with feedback, complete positive vectors once labels discussed, additional corpus details).
- Keep v8 implementation blocked pending ADR gates.

---

## 2026-11-28 — Expanded COSE v2 negative-test mutations (invalid point, wrong curve, indefinite length, duplicate keys)

### Observation / Action

- Negative-test outline still lacked concrete illustrations for invalid-point, wrong-curve, indefinite-length, and duplicate-key rejection cases.
- Added concrete examples derived from KAT 0001: mutated x-coordinate (invalid point), crv changed from 1 to 2 (wrong curve), indefinite-length bstr description, and duplicate-key map description.
- Documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Reviewers have additional actionable mutation illustrations for all fifteen checklist items without any claim of parser implementation or profile approval.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (refine schema with feedback, complete positive vectors once labels discussed, additional corpus details).
- Keep v8 implementation blocked pending ADR gates.

---

(Previous entries preserved in full history; this is the latest engineering log entry.)
