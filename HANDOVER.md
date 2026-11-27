## 2026-11-27 — CI: fix emulator instrumentation retry script syntax

- Previous hardening commit used a multi-line `for` loop in the `script:` block of `reactivecircus/android-emulator-runner@v2`. The action splits multi-line scripts and runs each line with `sh -c`, producing `Syntax error: end of file unexpected (expecting "done")` on run #46035651581.
- Collapsed the single-retry wrapper into one shell command so it executes correctly. Emulator resource limits (`-memory 2048 -cores 2`), `-gpu off`, and extended boot timeout remain.
- No runtime, schema, cryptographic, or authorization changes. Room remains v7.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft with green CI on its head.
- Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-27
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Confirm the new CI run on the fixed retry script remains green. Continue protocol-review support (full payload schemas, malformed-input corpus details, ADR clarity) without claiming production COSE/E2EE/P2P. Keep v8 implementation blocked pending ADR gates.
