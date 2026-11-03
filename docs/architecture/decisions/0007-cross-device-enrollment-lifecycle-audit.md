# ADR 0007 — Cross-device enrollment lifecycle and owner inbox state (audit)

- Status: AUDIT / DESIGN PROPOSAL — review required; no runtime behavior approved
- Date: 2026-11-03
- Scope: local persistence lifecycle across applicant and initial-owner installations
- Depends on: ADR 0005 and ADR 0006

## Context

The v1 enrollment flow transfers four artifacts manually. The same logical
request exists in two independent Room databases: first as an applicant-side
record and, after proof import, as an owner-side pending request. A local
database row must not be assumed to synchronize between devices.

This audit traced the Compose actions, identity services, repository
interfaces, Room DAO transactions, and the current tests.

## Source-backed cross-device flow

1. **Applicant creates FMER1.** The applicant signs a request. Its local
   identity is not yet a member.
2. **Owner imports FMER1.** DeviceEnrollmentProofAcceptanceService verifies
   the proof and atomically consumes the nonce while storing a pending row in
   the owner's local Room database.
3. **Owner issues FMOD1.** EnrollmentDecisionSigner signs an approval or
   rejection after checking the local initial-owner anchor and request
   interval. The Compose UI displays the generated text for manual transfer.
   The owner-side action does not persist the issued decision or change the
   owner-side pending row.
4. **Applicant imports FMOD1.** EnrollmentDecisionAcceptanceService
   verifies the decision against the applicant's local pinned owner and its
   local pending request. RoomEnrollmentDecisionRepository records the
   decision and deletes the applicant-side pending row in one transaction.
   This is a different database from the owner's.
5. **Applicant may create FMJA1.** The applicant signs an acceptance of an
   approved decision. The signer itself does not persist the acceptance.
6. **Owner imports FMJA1.** EnrollmentFinalizationService verifies the
   embedded owner decision and applicant signature against the owner's local
   pending row and local owner anchor. RoomEnrollmentFinalizationRepository
   atomically inserts the authenticated-device record and deletes the
   owner-side pending row.

Therefore, applicant-side decision persistence deleting its local pending
row does **not** remove the owner's independent pending row. The apparent
conflict between decision acceptance and owner finalization is not a
same-database contradiction.

## Confirmed lifecycle gap

The owner-side generated-decision path has no corresponding persistence
transition:

- After the owner generates either an approval or rejection, the owner-side
  pending row remains unchanged.
- The owner inbox UI derives its list from that pending-row flow and continues
  to label the request as awaiting owner review.
- On rejection, the applicant can import and persist the rejection, but no
  artifact is transferred back to update the owner's local request state.
  The owner-side row therefore remains pending.
- On approval, retaining the owner-side row is necessary for the current
  FMJA1 finalization verifier. If the applicant never accepts, the row also
  remains pending.
- Room currently prunes expired nonce rows during later nonce consumption;
  it has no corresponding pending-request expiry cleanup.
- The current unit tests exercise decision acceptance and finalization with
  separate fakes, but do not model two independent installations or the
  owner's issued-decision/inbox state transition. Room transaction behavior
  is not covered by Android instrumentation in the current CI workflow.

This is a persistence/UI lifecycle issue, not evidence that the cryptographic
checks are bypassed. It does mean the owner inbox can retain stale or
misleading pending items, including rejected requests.

## Design constraints for a follow-up

Do not solve this by deleting the owner-side pending row immediately after
issuing an approval: the current owner-side finalization path needs the
original request to verify FMJA1.

Before changing schema or runtime behavior, choose and document:

1. Whether the owner should persist an issued decision separately from the
   still-finalizable request, or add an explicit owner-side request state.
2. Whether a rejection should immediately resolve/remove the owner inbox item,
   and how that resolution is authenticated or otherwise made trustworthy.
3. Whether an approval remains finalizable until expiry, whether a signed
   pre-expiry acceptance may arrive after expiry, and when stale owner rows
   are pruned.
4. How repeated issuance, exact replay, conflicting decisions, app restart,
   and interrupted transactions behave.
5. Whether owner-side decision history is retained after finalization or
   rejection, and for how long.

A likely design direction is to separate the immutable request facts needed
for verification from the mutable inbox/lifecycle status, and to retain the
signed issued decision as an auditable record. This is a proposal only; it is
not a schema mandate.

## Required tests before implementation

- Two independent local stores: applicant decision import must not mutate the
  owner's pending request.
- Owner issues approval: owner request remains available for FMJA1
  verification, while inbox status no longer says "awaiting review".
- Owner issues rejection: owner inbox reaches the explicitly approved
  terminal/rejected state without falsely creating membership.
- Applicant imports approval/rejection: only applicant-local request state
  changes; exact decision replay is idempotent.
- Applicant signs acceptance, transfers it, and owner finalizes using the
  independent owner-local request.
- Missing owner request, expired request, wrong family/request/key, wrong
  decision, and conflicting finalization fail closed.
- App restart between every transfer step preserves the intended state.
- Transaction interruption cannot leave a decision/status update half-applied.
- Expiry cleanup removes or archives stale pending rows according to the
  approved policy, without deleting records still required for audit or
  finalization.
- Android Room instrumentation verifies migrations, DAO transactions, and
  concurrent/replayed operations.

## Explicit non-decisions

This audit does not change v1 behavior, select an expiry/receipt policy,
authorize a Room schema migration, define distributed synchronization, or
approve production authorization. The custom v1 transfer protocol remains
distinct from the proposed COSE v2 profile.
