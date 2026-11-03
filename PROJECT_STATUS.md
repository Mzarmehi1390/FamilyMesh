# FamilyMesh Project Status

Last verified: 2026-11-03
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Branch: `main`
Latest verified source commit: `21dd7ca4db718ee31416e45319d88ffce9c950c3`
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
| Room / DataStore local storage | IMPLEMENTED_AND_TESTED | Room schema v7; instrumentation still needed |
| Android Keystore device signing | IMPLEMENTED_UNVERIFIED | P-256-only verification enforced; unit tests; device/API instrumentation pending |
| Signed enrollment proof | IMPLEMENTED_AND_TESTED | Bounded local codec and verifier tests |
| Local nonce replay protection | IMPLEMENTED_AND_TESTED | One database only; not distributed |
| Initial-owner trust bootstrap/pinning | IMPLEMENTED_AND_TESTED | Manual out-of-band fingerprint comparison |
| Signed owner decision | IMPLEMENTED_AND_TESTED | Service resolves the pin itself; tests reject missing, mismatched, and ambiguous pins |
| Applicant acceptance + owner finalization | IMPLEMENTED_AND_TESTED | Both signatures verified; local Room v7 record |
| Production membership authorization | BLOCKED | No trusted resolver; default denies all |
| Canonical COSE protocol | IN_PROGRESS | Draft v2 profile, one primitive known-answer fixture, schema-parity review, and source-backed v1 field inventory and v2 time/text proposal (including signed-event vs local-receipt semantics and pending-expiry lifecycle gap) documented; production implementation and independent interoperability validation not complete |
| Invitations / revocation / recovery | NOT_STARTED | Requires reviewed lifecycle and authority model |
| E2EE messaging | NOT_STARTED | No message crypto implementation |
| BLE / Wi-Fi Direct / internet P2P / relay | NOT_STARTED | No transport implementation |
| Cross-device synchronization | NOT_STARTED | Manual text transfer only |
| Gradle Wrapper | NOT_STARTED | CI installs Gradle 8.13 |
| Android instrumentation / device validation | NOT_STARTED | Not demonstrated by current CI |

## Latest verification

CI run [#41672460808](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41672460808)
completed successfully for the documentation baseline:
- `testDebugUnitTest`
- `assembleDebug`
- debug APK artifact upload

This is CI evidence only; it is not a production security review or device
instrumentation result. Documentation commits after the last verified baseline are not covered by
that successful run. The latest lifecycle-audit commit is 86cc551; Android CI
run #41677314967 was queued/in progress at the last check, so its result is
not yet confirmed.

## Immediate priority

Complete the Identity & Trusted Enrollment protocol review. Keep authorization
integration and network transport blocked until canonical encoding, key IDs,
signature representation, replay scope, trust ceremony, and recovery/
revocation semantics have been reviewed.

## Protocol audit progress

- ADR 0008 proposes a separate owner-issued FMOD1 ledger. The existing
  `enrollment_decisions` table is applicant-side acceptance state and must
  not be repurposed. Proposed lifecycle states and exact-artifact matching
  at FMJA1 finalization are documented; no runtime/schema change has been
  made. Expiry policy remains unresolved under ADR 0006.

- ADR 0007 traces the separate applicant and owner Room stores. Applicant-side
  decision import and owner-side finalization use independent local rows; the
  apparent same-database conflict is not real. A separate owner-inbox gap is
  confirmed: issuing FMOD1 does not persist/update owner-side status, so
  rejected requests remain shown as awaiting review. Proposed remediation
  and integration/Room instrumentation tests are documented; no runtime
  change has been made.

- Four signed v1 envelope decoders enforce canonical unpadded Base64URL.
- All four signature verification paths reject non-P-256 EC keys.
- Custom fixed-order payloads remain non-COSE and are not approved as the
  production wire format.
- Current key IDs are domain-separated SHA-256 fingerprints over X.509 SPKI;
  JCA ECDSA signatures remain DER encoded.
- Nonce replay storage is atomic but installation-local.
- ADR 0003 provides a concrete draft profile for COSE v2; it is not yet an
  accepted production protocol.


## Room validation and expiry-policy update — 2026-11-03

- ADR 0009 documents the verified Room schema v7 migration chain and the
  missing SQLite instrumentation coverage in CI. It proposes, but does not
  implement, an additive v7 → v8 migration for a future owner-issued
  decision ledger.
- ADR 0006 now explicitly compares owner-receipt-deadline and signed-event-
  deadline expiry policies, including offline-delivery consequences and
  required product/security decisions. Neither policy is selected.
- Corrected the malformed T3 formatting in ADR 0006.
- ADR 0009 CI #41678430461 and Work Log CI #41678451624 passed. The ADR 0006
  clarification is a newer commit and its CI result remains to be checked.
- No runtime, Room schema, protocol, or v1 expiry behavior changed.
