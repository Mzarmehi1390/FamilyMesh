# FamilyMesh Handover

Last synchronized: 2026-11-03
Repository: https://github.com/Mzarmehi1390/FamilyMesh
Default branch: `main`

## Last verified repository state

- Branch: `main`
- Latest documentation commit: `be4a85cd7d2b357e3fb356c2834d94491c59db95`
- Latest verified source commit: `21dd7ca4db718ee31416e45319d88ffce9c950c3`
- Latest CI run checked: [Android CI #41672460808](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41672460808)
- CI result: completed successfully; unit tests, debug APK assembly, and artifact upload all passed.
- Branch protection: disabled; no open issues or pull requests were present at the prior synchronization.

## Product and architecture invariants

- Native Android only: Kotlin, Jetpack Compose, Gradle Kotlin DSL.
- Preserve modular architecture: `app`, `core:common`, `core:storage`,
  `core:designsystem`, `core:identity`.
- FamilyMesh remains private, decentralized, and end-to-end encrypted by
  design. Do not replace P2P with a centralized messaging backend.
- Never claim E2EE, network P2P, BLE, Wi-Fi Direct, relay, or production
  membership authorization exists; those are not implemented.
- Keep all authorization fail-closed until a reviewed trusted membership
  resolver exists.
- Do not silently replace a lost/invalidated device key.

## Completed implementation visible at the latest verified source commit

- Native Android foundation, Compose UI, Koin dependency injection.
- Room family-space and local member metadata persistence; Preferences
  DataStore.
- Android Keystore P-256 ECDSA signing identity and typed lifecycle policy.
- Signed device-enrollment request with nonce and expiry.
- Bounded manual enrollment proof text codec and local nonce replay storage.
- Local initial-owner bootstrap and signed trust bundle.
- Recipient-side owner fingerprint confirmation and immutable local pin.
- Signed owner decision (`FMOD1.`) verified against the pinned owner and
  exact pending request; persisted atomically.
- Applicant-signed acceptance (`FMJA1.`) bound to the exact approved decision.
- Owner-side verification of both signatures and local pending request.
- Room v7 authenticated-device record insertion and pending-request consumption
  in one transaction.
- Home screen lists authenticated devices for the selected family.
- Decision acceptance service resolves the owner key from
  `InitialOwnerPinRepository` internally; UI callers cannot inject an
  arbitrary owner trust record.
- All four enrollment transfer codecs reject non-canonical/padded Base64URL aliases.
- All enrollment signature verification boundaries reject non-P-256 EC public keys.
- Unit tests cover domain, identity, and local persistence behavior.

## Latest code changes and verification

- Commits: `918b03b` (canonical Base64URL), `36e2d27` (P-256 enforcement), `21dd7ca` (parser cleanup).
- CI #41671671596 and #41671944679 passed their respective security-hardening changes.
- CI #41671995919 passed unit tests, APK assembly, and artifact upload for the latest source commit.
- CI #41672460808 completed successfully for the earlier documentation baseline. Later docs commits through ADR 0005 have no status entries exposed by the available status/PR-run queries; CI is unconfirmed.

## Current work package

**WP — Identity and trusted enrollment protocol review.**

The local enrollment prototype is implemented, but its custom binary payloads
are not the proposed interoperable COSE protocol. ADR 0003 records a
draft v2 profile; primitive KAT 0001 now provides a reproducible signature/encoding fixture.
It remains a proposal: no protocol migration or authorization integration
should be implemented until independent vectors and platform tests validate it.

## Open blockers and risks

1. ADR 0001 remains proposed: code uses JCA P-256 / SHA256withECDSA and
   X.509 public-key bytes with custom domain-separated length-prefixed
   payloads; COSE ESP256 serialization/conversion is not implemented.
2. Nonce consumption is local to one Room database; it is not distributed
   replay protection.
3. Initial-owner trust pinning is a manual out-of-band fingerprint ceremony;
   ownership transfer and multi-owner semantics are undefined.
4. No production `TrustedFamilyMembershipResolver` exists;
   `DenyAllFamilyMembershipResolver` remains the default.
5. No device revocation, recovery, cross-device synchronization, network
   transport, BLE, Wi-Fi Direct, internet P2P, relay, or E2EE messaging.
6. Keystore behavior has not been verified by Android API 31+ instrumentation
   tests on physical/emulated devices.
7. Room migrations and transaction behavior need Android instrumentation
   coverage.
8. Gradle Wrapper is absent. CI currently installs Gradle 8.13 and JDK 17.
9. Original Master Engineering Prompt was not present among the accessible
   repository/project files during this synchronization. Do not invent its
   contents; use the repository roadmap and established project invariants.

## Next exact engineering action

Review ADR 0003's COSE v2 profile and produce cross-language known-answer
vectors for the key representation, protected headers, external AAD,
Sig_structure, and ECDSA DER/R||S conversion. Then add parser and malformed
input tests before any migration implementation. Keep networking and
authorization integration blocked until replay scope, trust lifecycle,
revocation, and recovery are also resolved.


## Latest lifecycle audit (documentation proposal)

- ADR 0007 traces the separate applicant and owner Room stores through FMER1,
  FMOD1, and FMJA1. Applicant-side decision import deleting its local pending
  row does not remove the owner's independent request row.
- A concrete owner-inbox gap remains: creating FMOD1 does not persist an
  owner-side decision or update inbox status. Rejected requests remain
  labelled as awaiting review; approved requests remain indistinguishable
  until the owner receives FMJA1 and finalizes.
- Pending-request expiry cleanup is still absent. Do not delete owner pending
  rows on approval issuance until a reviewed design preserves the request
  facts required by FMJA1 verification.
- ADR 0007 is an audit/design proposal only. No runtime or schema change was
  made. Required next work: decide owner-side lifecycle states, then implement
  cross-device state tests and Room instrumentation tests.


## 2026-11-03 — Room and expiry review handover

- ADR 0009 records the current Room v7 migration chain and the fact that
  current CI does not run Android instrumentation tests. It defines the
  migration and transaction test acceptance criteria for a future owner
  decision ledger; it does not change schema or runtime.
- ADR 0006 now compares receipt-deadline and signed-event-deadline expiry
  policies. No policy or numeric TTL/skew value has been selected.
- Before implementing owner-ledger persistence, obtain product/security
  answers to the offline-delivery, expiry-purpose, delivery-delay,
  clock-rollback, and retention questions in ADR 0006.
- Keep schema changes additive, preserve applicant-side
  `enrollment_decisions` semantics, and require exact owner-issued FMOD1
  matching before FMJA1 finalization.
- CI #41678430461 (ADR 0009) and #41678451624 (Work Log) passed. Verify CI
  independently for the later ADR 0006 clarification commit.


## 2026-11-03 — Enrollment clock trust-boundary handover

- ADR 0010 traces `nowEpochMillis` through proof receipt, decision import, and owner finalization services to the Compose call sites. Current UI supplies `System.currentTimeMillis().coerceAtLeast(0L)`; services do not independently authenticate or acquire time.
- Treat this as a mutable wall-clock/caller-boundary limitation, not proof that current UI accepts arbitrary timestamp text. A clock abstraction alone is not a security-grade anti-rollback source.
- Keep signed event timestamps separate from local receipt/recording timestamps.
- Do not change expiry/finalization behavior or add cleanup until ADR 0006's policy and threat model are accepted. Then review ADR 0008/0009 before implementing owner ledger/schema work.
- ADR 0006 clarification CI #41680089589 and Work Log CI #41680127933 passed. ADR 0010: `0d7271217f308d7f854555bc7b5871b5628550a8`.
