# ADR 0011: Exactness of Owner-Side Finalization Replay

- Status: Proposed; source audit
- Date: 2026-11-03
- Scope: v1 owner-side FMJA1 finalization idempotency
- Related: ADR 0007 (cross-device lifecycle), ADR 0008 (owner-issued decision ledger), ADR 0009 (Room instrumentation), ADR 0010 (clock trust boundary)

## Context

Owner-side finalization is intended to be idempotent: re-importing the same valid FMJA1 acceptance after a successful finalization should report `AlreadyFinalized`, while a different artifact for the same request should not be reported as success.

The current implementation must be checked against that exact-artifact property, not merely against equality of one signature byte string.

## Source-backed finding (main branch, 2026-11-03)

In `EnrollmentFinalizationService.finalize`:

1. The service looks up the pending request.
2. If no pending request exists, it immediately calls `repository.findFinalized(familyId, requestId, applicantDeviceKeyId, signatureBase64)` and maps the result.
3. The owner trust anchor, local owner identity, owner decision signature, applicant public-key fingerprint, and applicant acceptance signature are verified only when a pending request exists.

In `RoomEnrollmentFinalizationRepository.findFinalized`:

- The stored record is selected by family/request.
- `ALREADY_FINALIZED` is returned when stored `deviceKeyId` equals the supplied applicant key ID and stored `acceptanceSignatureBase64` equals the supplied signature bytes encoded as Base64.
- It does not compare the complete FMJA1 artifact, the acceptance signing payload, applicant public-key bytes, signed acceptance timestamp, nested FMOD1 artifact, or a digest of those fields.

The current database record stores the applicant public key, role, joined time, and acceptance signature, but not the exact accepted FMJA1 text or a digest of its complete signed payload.

The existing unit test `exactReplayAfterFinalizationIsIdempotent` covers an unchanged replay only. It does not test a modified envelope that retains the same signature bytes.

## Consequence

An altered FMJA1 envelope that retains the original applicant key ID and signature bytes can reach the already-finalized fast path without cryptographic verification. Since the signature is over the acceptance payload, a modified payload would not be a valid acceptance; nevertheless, the current fast path can report `AlreadyFinalized` based on the partial stored comparison.

This does not by itself create a second membership row or alter the already stored membership. The confirmed issue is an idempotency/result-integrity gap: the service may report success for an artifact that is not byte-for-byte or semantically identical to the one previously finalized.

## Required design before a runtime fix

1. Define the replay identity as the exact canonical FMJA1 artifact or a collision-resistant digest of the complete canonical acceptance payload, including the nested FMOD1 artifact and all acceptance fields.
2. Preserve the current membership row as historical evidence; do not infer the original complete acceptance artifact from the signature alone.
3. For new records, persist the exact canonical artifact or its domain-separated digest atomically with membership finalization.
4. For legacy rows that lack this replay identity, fail closed for exact-replay claims or use an explicitly documented compatibility outcome. Do not silently upgrade a partial match into an exact match.
5. Keep signature verification and owner-ledger matching mandatory for first-time finalization. The exact replay path must not turn an invalid envelope into a success response.
6. Ensure the repository transaction enforces one stable replay identity per `(familySpaceId, requestId)` and rejects a different artifact as conflict.
7. Add unit and Room instrumentation tests for:
   - unchanged canonical replay;
   - same signature with modified acceptedAt;
   - same signature with modified applicant public key;
   - same signature with altered nested FMOD1 fields/text;
   - different valid acceptance for the same request;
   - legacy finalized row without replay identity;
   - process restart and concurrent duplicate imports.
8. Decide whether the external result should distinguish `ALREADY_FINALIZED_EXACT_REPLAY` from legacy/unknown replay identity, rather than returning a generic success-like state.

## Decision

No runtime, schema, or protocol changes are made by this audit. This ADR records a source-backed correctness gap and the requirements for a backward-compatible remediation. It is independent of ADR 0006's unresolved expiry policy, although any future schema migration should be coordinated with ADR 0008 and ADR 0009.
