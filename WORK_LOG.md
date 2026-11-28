# FamilyMesh Work Log

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

## 2026-11-28 — Expanded COSE v2 negative-test mutations

### Observation / Action

- Negative-test outline still lacked concrete illustrations for several high-value rejection cases.
- Added concrete examples for algorithm substitution (alg -9 → -7), unknown label addition, non-deterministic map reordering, and missing CBOR tag 18 (from KAT 0001 bytes).
- Documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Reviewers have additional actionable mutation illustrations without any claim of parser implementation or profile approval.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (refine schema with feedback, complete positive vectors once labels discussed, additional corpus details).
- Keep v8 implementation blocked pending ADR gates.

---

## 2026-11-28 — Low-S signature-conversion review notes and kid-mismatch example

### Observation / Action

- Protocol review still needed concrete guidance on DER-to-fixed-width R||S conversion and the proposed low-S policy.
- Added a documentation-only review note covering conversion steps, rejection criteria, high-S construction from existing KATs, and a concrete kid-mismatch mutation.
- Linked the notes from ADR 0003 and the negative-test outline.
- Documentation-only. No runtime, schema, cryptographic implementation, or authorization changes.

### Impact

- Reviewers now have explicit conversion and high-S construction guidance without any claim of parser implementation or profile approval.
- Room remains v7; ADR 0006/0008/0012 implementation gates unchanged. Authorization remains fail-closed.

### Next

- Continue protocol-review support (refine schema with feedback, complete positive vectors once labels discussed, additional corpus details).
- Keep v8 implementation blocked pending ADR gates.

---

(Previous entries preserved in full history; this is the latest engineering log entry.)
