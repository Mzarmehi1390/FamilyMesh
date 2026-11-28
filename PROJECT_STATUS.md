## 2026-11-28 — Expanded COSE v2 negative-test mutations

- Negative-test outline now includes concrete mutation examples for algorithm substitution, unknown label, non-deterministic map, and missing-tag cases (from KAT 0001).
- Documentation-only; does not implement conversion or approve the draft profile.
- Latest Android CI on develop/agent-ready remains green.
- Integration candidate [PR #2](https://github.com/Mzarmehi1390/FamilyMesh/pull/2) remains Draft with green CI on head `f47634412bf0f2c9b967a1f55fe6cdf567b79244`.
- Room remains v7. Do not implement the proposed v8 owner-decision ledger/FMJA1 digest or alter expiry semantics until the ADR 0006/0008/0012 gates are explicitly approved. Keep membership authorization fail-closed and never replace a lost device key.

# FamilyMesh Project Status

Last verified: 2026-11-28
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Branch: `main` (source) / `develop/agent-ready` (docs + fixtures + CI hardening)
Latest repository commit on main: `968b580283b2ce17d0deda83beab4675ec55b845`
Integration candidate: PR #2 head `f47634412bf0f2c9b967a1f55fe6cdf567b79244` (green CI)
Protocol review drafts: ADR 0003 and ADR 0006 (proposals only; not production-approved)

## Overall state

Milestone 1 — Domain and identity is in progress. The Android foundation is
in place and a local manually transferred device-enrollment prototype now
reaches owner-side authenticated-device persistence.

This is not a production-ready messenger. End-to-end message encryption,
peer-to-peer transport, relay, synchronization, revocation, recovery, and
production authorization are not implemented.

## Work package classification

| Area | Status | Evidence / limitation |
|---|---|---|
| Native Android + Compose foundation | IMPLEMENTED_AND_TESTED | CI builds debug APK |
| Modular Gradle architecture | IMPLEMENTED_AND_TESTED | Five modules present |
| Room / DataStore local storage | IMPLEMENTED_AND_INSTRUMENTED | Room schema v7; historical fixtures v1–v7 checked in; MigrationTestHelper coverage present and CASCADE assertions now correct; emulator job hardened |
| Android Keystore device signing | IMPLEMENTED_AND_INSTRUMENTED | P-256-only verification enforced; API 31 and API 34 instrumentation green on PR #2 |
| Signed enrollment proof | IMPLEMENTED_AND_TESTED | Bounded local codec and verifier tests |
| Local nonce replay protection | IMPLEMENTED_AND_TESTED | One database only; not distributed |
| Initial-owner trust bootstrap/pinning | IMPLEMENTED_AND_TESTED | Manual out-of-band fingerprint comparison |
| Signed owner decision | IMPLEMENTED_AND_TESTED | Service resolves the pin itself; tests reject missing, mismatched, and ambiguous pins |
| Applicant acceptance + owner finalization | IMPLEMENTED_AND_TESTED | Both signatures verified; local Room v7 record |
| Production membership authorization | BLOCKED | No trusted resolver; default denies all |
| Canonical COSE protocol | IN_PROGRESS | Draft v2 profile + four primitive KATs (all artifact contexts) + negative-test outline with concrete mutation examples (including algorithm, unknown label, non-deterministic, missing tag) + review-only payload schema proposal + low-S conversion notes; not production |
| Invitations / revocation / recovery | NOT_STARTED | Requires reviewed lifecycle and authority model |
| E2EE messaging | NOT_STARTED | No message crypto implementation |
| BLE / Wi-Fi Direct / internet P2P / relay | NOT_STARTED | No transport implementation |
| Cross-device synchronization | NOT_STARTED | Manual text transfer only |
| Gradle Wrapper | IN_PROGRESS | Pinned 8.13 wrapper on PR #2 (green CI); not yet on main |
| Android instrumentation / device validation | PARTIALLY_VERIFIED | Storage job on this branch now includes emulator resource and retry hardening (single-command script); full suite green on PR #2 candidate |

## Immediate priority

Complete the Identity & Trusted Enrollment protocol review. Keep authorization
integration and network transport blocked until canonical encoding, key IDs,
signature representation, replay scope, trust ceremony, and recovery/
revocation semantics have been reviewed.

Do not implement Room v8 until ADR 0006/0008/0012 gates are satisfied.
