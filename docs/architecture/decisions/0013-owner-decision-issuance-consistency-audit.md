# ADR 0013: Owner Decision Issuance and Cross-Installation Consistency Audit

- Status: Source audit / design findings; no runtime behavior approved
- Date: 2026-11-03
- Scope: owner-side FMOD1 issuance, applicant-side import, and owner-side FMJA1 finalization
- Related: ADR 0007, ADR 0008, ADR 0011, ADR 0012

## Source-backed findings

### 1. Owner issuance is transient and repeatable

The owner actions in `FamilyMeshHomeScreen` call `EnrollmentDecisionSigner.sign` for APPROVED or REJECTED, encode the returned FMOD1, and place it in Compose state. They do not persist the issued decision. The pending owner request remains in the inbox, and `canDecide` is based on owner identity and the local wall-clock comparison with request expiry; it does not check whether a decision has already been issued.

Consequently, the owner UI can issue multiple distinct signed decisions for the same family/request, including opposite outcomes. The generated artifact is transient and can be lost on process recreation. There is no durable record identifying which decision the owner intended as authoritative.

### 2. Applicant import is terminal locally, but does not synchronize owner state

`EnrollmentDecisionAcceptanceService` verifies an imported decision against the applicant installation's pinned owner and pending request. `RoomEnrollmentDecisionRepository.persistDecision` inserts the applicant-side decision and deletes that installation's pending request in one transaction. This means a second, different decision for the same request is not accepted through that same local pending-request path after the first import.

This is a local-installation transaction only. It cannot update the owner's separate Room database. Do not describe applicant-side deletion as synchronization or owner-side revocation.

### 3. Owner finalization accepts any valid owner-signed decision matching the request

`EnrollmentFinalizationService` verifies the FMJA1 nested decision signature using the locally anchored owner key and verifies the applicant acceptance against the owner-side pending request. It does not query an owner-issued decision ledger because none exists today.

Therefore, if the owner has signed both approval and rejection artifacts for one request, an applicant acceptance embedding the earlier approval can still pass the current owner-side cryptographic/request checks while the owner pending request remains. The later rejection is not a revocation signal and does not invalidate the earlier approval. The system has no durable single-authoritative-decision rule.

This is a decision-consistency and lifecycle gap. It is not a signature forgery: both artifacts are genuinely signed by the trusted owner. It can result in a membership finalization that conflicts with the owner's later rejection intent.

### 4. Exact replay is a separate issue

ADR 0011 documents the finalized fast-path comparison of applicant key ID and signature bytes. ADR 0012 proposes a complete canonical FMJA1 replay digest. Neither solves which FMOD1 artifact was authoritatively issued before finalization. The owner-issued ledger and replay identity solve distinct invariants and both are required in the future persistence design.

## Required invariant for the future owner ledger

For each `(familySpaceId, requestId)`, at most one distinct owner-issued FMOD1 artifact may become authoritative in v1.

- First issuance: verify the local initial-owner anchor and exact pending request; sign; persist exact canonical FMOD1 before reporting success or displaying it as issued.
- Exact reissue: return the stored artifact; do not create a new signature/artifact.
- Different outcome or different artifact: return conflict; never overwrite the first durable decision.
- Approval: preserve request facts required for FMJA1 verification.
- Rejection: terminal for finalization; any FMJA1 embedding it or an earlier/nonmatching approval fails closed.
- Finalization: require exact equality between FMJA1's nested FMOD1 text and the durable owner-issued APPROVED artifact, in addition to all existing cryptographic checks.
- Finalization, replay identity persistence, and lifecycle resolution must be atomic.

A later revocation feature would require its own explicit signed protocol/state transition; merely issuing another FMOD1 must not be treated as revocation.

## UI and recovery requirements

- After durable issuance, show the request as `APPROVED_WAITING_ACCEPTANCE` or `REJECTED`, not `PENDING_REVIEW`.
- Keep the stored FMOD1 available after process death so the owner can copy/re-send the same artifact.
- Disable a second conflicting decision action for a request with a stored decision.
- Storage failure must not show an issued/success state or expose an artifact as durably issued.
- Approval remains eligible for finalization only under the expiry policy selected by ADR 0006.
- Rejection must not create membership and must remain terminal independently of applicant-side state.

## Required tests

1. Owner issues approval, process restarts, and the exact same FMOD1 can be re-exported.
2. Owner issues approval then attempts rejection: conflict; stored approval remains unchanged.
3. Owner issues rejection then attempts approval: conflict; stored rejection remains unchanged.
4. FMJA1 embedding the exact stored approval can finalize once.
5. FMJA1 embedding a valid but unrecorded owner-signed approval fails closed.
6. FMJA1 embedding a rejection fails closed.
7. FMJA1 embedding a different valid approval for the same request fails closed.
8. Applicant imports a decision: only applicant-local state changes; owner-local ledger/inbox is unchanged until artifact transfer/finalization.
9. Process death and transaction failure at each issuance/finalization boundary preserve a coherent ledger, request, inbox, membership, and replay identity.
10. Concurrent issuance of opposite outcomes yields exactly one durable winner and one conflict.

## Implementation boundary

Do not patch this by adding a UI-only disabled flag or deleting pending requests after issuance. The invariant must be enforced by a unique owner-side persistence key and transaction, and finalization must compare the exact persisted artifact. Coordinate implementation with ADR 0012's single Room 7 → 8 migration. Keep ADR 0006 expiry policy separate.

No runtime, database, protocol, cryptographic, or transport changes are made by this audit.
