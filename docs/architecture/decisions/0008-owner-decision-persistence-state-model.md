# ADR 0008 — Owner-side decision persistence and enrollment state model

- Status: DESIGN PROPOSAL — not approved for implementation
- Date: 2026-11-03
- Depends on: ADR 0005, ADR 0006, ADR 0007

## Problem statement

The existing `enrollment_decisions` table and
`RoomEnrollmentDecisionRepository` represent decisions imported and accepted
on the applicant installation. They are not an owner-side issuance ledger.
The owner Compose action currently signs FMOD1 and places the encoded text in
transient UI state only. There is no durable owner-side decision record and
no owner inbox status transition.

Reusing the applicant-side repository would conflate two different trust
boundaries and installations. Deleting the owner pending request on approval
would also break the current FMJA1 verifier, which needs the original request.

## Proposed separation of concerns

Keep three distinct records/concepts:

1. **Request facts** — the immutable owner-local facts accepted from FMER1.
   These remain available while an approval can still be finalized.
2. **Issued owner decision** — the exact signed FMOD1 artifact created by the
   trusted initial-owner device, durably stored on that same installation.
3. **Inbox lifecycle status** — a derived or transactionally maintained state
   used for display and eligibility; it must not replace or mutate signed
   request facts.

Do not rename or repurpose the existing applicant-side
`enrollment_decisions` table.

## Proposed owner-side decision ledger

Add a separate owner-side table (provisional name
`owner_issued_enrollment_decisions`) keyed by
`(familySpaceId, requestId)`. Store at minimum:

- familySpaceId and requestId;
- applicantDeviceKeyId and ownerDeviceKeyId;
- outcome;
- decidedAtEpochMillis;
- exact canonical FMOD1 encoded text (or an exactly specified byte
  representation);
- recordedAtEpochMillis.

The exact encoded artifact is retained because FMJA1 currently binds the
exact nested FMOD1 text. Do not replace it with a digest without a protocol
version change and new interoperability vectors.

The table should have a foreign key to the family space, but should not
cascade-delete the immutable request facts except when the family space itself
is deleted. Indexes should support inbox ordering and outcome/status queries.

## Proposed state model

The owner inbox should distinguish at least:

- `PENDING_REVIEW`: request exists and no owner decision is recorded.
- `APPROVED_WAITING_ACCEPTANCE`: an approval is recorded; request facts
  remain available for FMJA1 verification.
- `REJECTED`: a rejection is recorded; no membership may be created from
  this request.
- `FINALIZED`: authenticated device membership was committed.
- `EXPIRED`: request is no longer eligible under the expiry policy.

Prefer deriving the first four states from the request, issued-decision, and
authenticated-device records where practical. If a materialized status column
is chosen, every transition must be atomic with the corresponding ledger or
membership write. Avoid two independently mutable sources of truth.

## Transaction and conflict rules

1. Issuing a decision first verifies the trusted initial-owner anchor and the
   exact local pending request, then signs FMOD1.
2. Persist the exact signed artifact before displaying it as successfully
   issued. A storage failure must not report a durable issuance.
3. At most one distinct owner decision may be recorded for a request in v1.
   Reissuing the exact same decision is idempotent; a different outcome or
   different signed artifact is a conflict. If deterministic idempotent
   issuance cannot be guaranteed by the signer, return the already stored
   artifact instead of signing again.
4. An approval preserves the owner request facts until finalization or
   expiry cleanup.
5. A rejection must be terminal for membership. Keep enough request and
   decision history for audit; do not let a later FMJA1 turn a rejected
   request into membership.
6. Finalization must verify the embedded decision cryptographically and
   compare it with the exact owner-side issued decision record before
   inserting membership. Missing or conflicting ledger state fails closed.
7. The membership insert, request lifecycle resolution, and retention of
   decision history must be one Room transaction.
8. Replay and concurrent issuance/finalization must have deterministic,
   tested outcomes.

## Expiry policy remains a blocking decision

ADR 0006 identified two valid but different policies:

- **Receipt deadline:** the owner must receive and finalize FMJA1 before
  request expiry.
- **Signed-event deadline:** an applicant acceptance signed before expiry
  may be received later, subject to a separately defined maximum delivery
  delay or retention policy.

Do not implement `EXPIRED` transitions until the policy is selected.
A signed timestamp alone does not establish when the owner received an
artifact. Record local receipt time separately if the product chooses a
receipt deadline.

## Migration and rollout plan

A future implementation should:

1. Add a new owner-issued-decision entity/table and migration; do not alter
   the semantics of the applicant-side decision table.
2. Add domain types and a dedicated repository for owner-side issuance.
3. Make owner decision issuance a use case that verifies, signs, persists,
   then returns the exact encoded artifact.
4. Change the inbox to show derived lifecycle state and disable decisions
   after a terminal outcome.
5. Require the owner-side ledger match during FMJA1 finalization.
6. Add a Room instrumentation suite for migrations, rollback, unique-key
   conflicts, replay, concurrency, and process restart.
7. Preserve fail-closed behavior for legacy pending requests with no issued
   decision record. Do not infer that an unrecorded UI-generated code was
   durably issued.

## Required test matrix

- Owner approval persists exact FMOD1 and inbox becomes
  `APPROVED_WAITING_ACCEPTANCE`.
- Owner rejection persists exact FMOD1 and inbox becomes `REJECTED`.
- Repeated exact issuance returns the stored artifact and is idempotent.
- Conflicting issuance is rejected without overwriting the first decision.
- Storage failure never displays success or loses the only durable artifact.
- Applicant-side decision import changes only applicant-local state.
- FMJA1 with exact stored approval finalizes once.
- FMJA1 with no owner ledger record, a rejected decision, or a different
  decision fails closed.
- Finalization replay is idempotent; conflicting device/key/role fails.
- Transaction rollback preserves a coherent inbox, ledger, request, and
  membership state.
- Migration from every supported schema version preserves existing data.
- Expiry tests cover both candidate policies, but production behavior is
  blocked until one is explicitly selected.

## Non-goals

This proposal does not implement distributed synchronization, change FMER1 /
FMOD1 / FMJA1 wire formats, change cryptographic algorithms, authorize
membership through a production resolver, or decide the expiry policy.


## Follow-up audit

ADR 0013 confirms from current source that owner issuance is transient, multiple opposite FMOD1 artifacts can be signed for the same pending request, and finalization does not compare the nested artifact with a durable owner-issued record. The implementation invariant in this proposal is therefore security-relevant to decision consistency: persist the first exact artifact, make conflicting issuance fail, and require exact ledger match at finalization. ADR 0013 is an audit, not approval to implement.
