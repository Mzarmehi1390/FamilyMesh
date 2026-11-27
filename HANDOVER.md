## 2026-11-27 — CI: harden storage instrumentation against flaky emulator system crashes

- Updated Android CI emulator configuration on `develop/agent-ready`: increased memory/cores, switched to `-gpu off`, extended boot timeout, and wrapped the instrumentation task in a single retry.
- Addresses the `INSTRUMENTATION_ABORTED: System has crashed` failure observed on run #46028589670 (previous head).
- No runtime, schema, cryptographic, or authorization changes. Room remains v7.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft.
- Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-27
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Confirm the new CI run on the emulator-hardening commit remains green. Continue protocol-review support (full payload schemas, malformed-input corpus details, ADR clarity) without claiming production COSE/E2EE/P2P. Keep v8 implementation blocked pending ADR gates.
