## 2026-11-28 — Expanded COSE v2 negative-test mutations (invalid point, wrong curve, indefinite length, duplicate keys)

- Added concrete examples for invalid-point (mutated x-coordinate), wrong-curve (crv 1 → 2), indefinite-length, and duplicate-key cases to the negative-test outline (derived from KAT 0001).
- Documentation-only protocol-review support. No parser, executable tests, or profile approval added.
- Latest Android CI on this branch remains green.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft with green CI on its head.
- Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-28
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Continue protocol-review support (refine schema proposal with reviewers, generate complete positive vectors once labels are discussed, additional malformed-input corpus details, ADR clarity) without claiming production COSE/E2EE/P2P. Keep v8 implementation blocked pending ADR gates. Confirm ongoing CI green on this branch.
