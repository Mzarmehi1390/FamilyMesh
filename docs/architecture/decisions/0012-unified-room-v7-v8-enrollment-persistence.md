# ADR 0012: Unified Room v7-to-v8 Enrollment Persistence

- Status: Proposed; schema design only
- Date: 2026-11-03
- Related: ADR 0007, 0008, 0009, 0011
- Depends on approval of ADR 0008. Does not resolve ADR 0006.

## Source findings

Room is currently schema version 7. The v6-to-v7 migration created `authenticated_family_devices` with primary key `(familySpaceId, requestId)` and unique index `(familySpaceId, deviceKeyId)`. Its rows retain applicant key ID, public key, role, accepted time, and signature, but not the complete FMJA1 artifact.

`EnrollmentFinalizationService` bypasses cryptographic verification when the pending request is absent and delegates replay recognition to a comparison of applicant key ID and signature. ADR 0011 records why that comparison is not exact artifact identity.

The FMJA1 codec deterministically encodes the envelope; decoding enforces canonical unpadded Base64URL, field bounds, supported version, and no trailing bytes. The envelope includes the nested canonical FMOD1 text. Re-encoding a decoded acceptance therefore provides the canonical v1 FMJA1 text.

The existing `enrollment_decisions` table is applicant-side state. It must not be repurposed as the owner's issuance ledger proposed by ADR 0008.

## Proposed single v7-to-v8 migration

If ADR 0008 is approved, combine both additive changes below in exactly one `MIGRATION_7_8`. Do not create competing migrations from version 7.

### 1. Exact FMJA1 replay identity

Add nullable `acceptanceReplayDigest TEXT` to `authenticated_family_devices`.

For newly finalized records, calculate lowercase hexadecimal SHA-256 over:

`UTF8("FamilyMesh:finalized-acceptance-replay:v1\\0") || UTF8(EnrollmentAcceptanceTextCodec.encode(acceptance))`

The domain separator ends in exactly one NUL byte. The digest covers the complete canonical FMJA1 envelope: nested FMOD1 text, applicant key ID, public-key bytes, accepted time, and signature. Compute it in the identity layer from the decoded acceptance using the canonical encoder. Never hash only the signature or a partial tuple.

This digest is an idempotency identifier, not authentication. First-time finalization must still verify the local owner trust anchor, owner decision signature, applicant key fingerprint, applicant acceptance signature, request binding, and exact owner-issued approval before persistence.

Replay rules:
- Same non-null digest for the same family/request: exact replay.
- Different digest: conflict.
- Legacy row with NULL digest: never claim exact replay; return a non-success compatibility/unknown result and preserve the membership row.
- Replay never inserts, replaces, or mutates membership.

The migration must leave every existing v7 row's digest NULL. Existing columns cannot reconstruct the original complete FMJA1 artifact, so no digest can safely be backfilled.

### 2. Separate owner-issued decision ledger

Create `owner_issued_enrollment_decisions` as proposed by ADR 0008, with composite key `(familySpaceId, requestId)`, applicant and owner key IDs, outcome, signed decision time, exact canonical FMOD1 text, local recorded time, and a cascading foreign key to `family_spaces(id)`. Add only indexes justified by actual inbox queries.

Do not alter `enrollment_decisions`; do not fabricate ledger rows for legacy requests or finalized devices. First-time FMJA1 finalization must match the nested FMOD1 to the exact locally issued approval.

## Migration constraints

- Increment Room exactly once, 7 to 8, and register one additive `MIGRATION_7_8`.
- Preserve all existing rows, keys, indexes, and foreign keys.
- Do not delete pending requests or infer old approvals, rejections, issuance, expiry, or replay identity.
- Keep destructive fallback disabled.
- Do not add expiry columns, cleanup, or EXPIRED states; ADR 0006 remains separate.

## Required tests

Unit tests:
- Digest stability for identical decoded acceptance.
- Changes to accepted time, applicant key ID, public key, signature, or nested FMOD1 change the digest.
- Digest is computed from canonical re-encoding with the versioned NUL-terminated domain separator.
- Invalid signatures and owner-ledger mismatch fail even if replay metadata is present.
- Legacy NULL digest never maps to exact-replay success.

Room instrumentation:
- Upgrade v7 fixture; preserve old authenticated devices with NULL digest.
- Persist membership and digest atomically.
- Exact replay succeeds; same signature with changed accepted time/public key/nested FMOD1 conflicts.
- Different valid artifact for same request conflicts.
- Concurrent finalization yields one stable result.
- Injected failure rolls back membership, digest, ledger/lifecycle, and pending-request changes.
- Reopen database and verify persisted state.
- Exercise fresh schema and historical fixtures v1-v7; verify foreign keys, indexes, and cascade behavior.

CI must retain unit tests and APK assembly and add the pinned-emulator instrumentation job required by ADR 0009. A green build alone is not migration validation.

## Not decided

This ADR does not approve the owner-ledger semantics, choose ADR 0006 expiry policy, change v1 cryptography or wire format, introduce distributed replay protection, or define ledger retention. Implementation starts only after ADR 0008 approval and review of this migration plan.
