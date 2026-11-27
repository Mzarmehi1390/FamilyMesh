## 2026-11-27 — CI stabilization applied to docs branch (API 31)

- Updated `.github/workflows/android-ci.yml` on `develop/agent-ready` (commit `dec517de`) to switch storage instrumentation to API 31 (minSdk), default target, AVD/system-image cache, longer boot timeout, and snapshot-disable.
- This addresses repeated emulator boot timeouts observed on API 34 runs (e.g. Android CI #45967740726).
- Feature branch `ci/stabilize-emulator-and-expand-negative-tests-20261127` (commit `8a2956a2`) contains the same CI change plus expanded CanonicalBase64Url / P256PublicKeyParser fail-closed unit tests. Draft PR creation presented for owner confirmation.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft with full green CI on head `f47634412bf0f2c9b967a1f55fe6cdf567b79244` (run #45943777424 succeeded).
- Room remains v7. Do not implement proposed v8 owner-ledger/FMJA1 digest or change expiry until ADR 0006/0008/0012 are explicitly approved. Authorization remains fail-closed; do not replace lost device keys.

# FamilyMesh Handover

Last synchronized: 2026-11-27
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Next exact engineering action

Confirm the next Android CI run on `develop/agent-ready` (and the dedicated CI branch once a PR is opened) is green for both `verify` and `storage-instrumentation`. Continue protocol-review support (additional KATs, ADR clarity) without claiming production COSE/E2EE/P2P. Advance Room migration fixtures and tests within ADR 0009/0014 bounds. Keep v8 implementation blocked pending ADR gates.
