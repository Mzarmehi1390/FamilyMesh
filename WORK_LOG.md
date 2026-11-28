# FamilyMesh Work Log

## 2026-11-28 — COSE v2 payload schema size estimates and encoding sketch

### Observation / Action

- Payload schema proposal still lacked concrete size guidance and an encoding example that reviewers could use to discuss field limits and overall object bounds.
- Added approximate maximum encoded sizes for each payload field (assuming preferred deterministic CBOR) and a worked CBOR encoding sketch for a minimal enrollment-proof payload under the candidate labels.
- Suggested a conservative 4 KiB starting bound for complete tagged COSE_Sign1 objects as a review discussion point.
- Documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Reviewers have additional concrete material for discussing payload sizes and overall object limits without any claim of parser implementation or profile approval.
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
