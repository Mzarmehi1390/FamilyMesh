## 2026-11-28 — Protocol review: candidate payload schema proposal + expanded mutations

- Added a draft candidate payload schema proposal (`cose-v2-candidate-payload-schema-proposal.md`) with integer labels, CBOR types, and length bounds derived from ADR 0005 inventory. Explicitly not frozen or approved.
- Expanded the negative-test outline with concrete examples for signature-length truncation, kid mismatch, and a high-S note.
- Documentation-only support for protocol review. No parser, executable tests, or profile approval added.
- Latest Android CI on the parent branch was green; this branch will be validated by the next CI run.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft with green CI on its head.
- Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-28
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Continue protocol-review support (refine candidate schema after review feedback, additional malformed-input corpus details, ADR clarity) without claiming production COSE/E2EE/P2P. Keep v8 implementation blocked pending ADR gates. Confirm CI green on this branch.
