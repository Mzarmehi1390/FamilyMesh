## 2026-11-28 — Protocol review: payload-schema proposal and expanded negative mutations

- Added a documentation-only COSE v2 payload schema proposal that maps the ADR 0005 source inventory to candidate CBOR labels, types, and byte limits for the four artifacts. Explicitly not approved for implementation.
- Expanded the negative-test outline with concrete protected-header bit-flip and signature-length mutation examples derived from KAT 0001, and linked the schema proposal.
- Documentation-only support for protocol review. No parser, executable tests, or profile approval added.
- Latest Android CI on this branch remains green.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft with green CI on its head.
- Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-28
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Continue protocol-review support (refine schema proposal with reviewers, generate complete positive vectors once labels are discussed, additional malformed-input corpus details, ADR clarity) without claiming production COSE/E2EE/P2P. Keep v8 implementation blocked pending ADR gates. Confirm ongoing CI green on this branch.
