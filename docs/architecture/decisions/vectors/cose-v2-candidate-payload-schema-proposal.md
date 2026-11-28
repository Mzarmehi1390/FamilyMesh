# COSE v2 candidate payload schema proposal

- Status: **Draft proposal for review only — not approved, not frozen, not implemented**
- Date: 2026-11-28
- Depends on: ADR 0003 (draft profile), ADR 0004 (parity review), ADR 0005 (source field inventory)
- Purpose: give reviewers a concrete starting point for integer labels, CBOR types, and length bounds so that full positive vectors can be generated later.

This document does **not** freeze any labels, does **not** authorize a parser, does **not** approve the draft profile, and does **not** change runtime, Room schema, or authorization. All choices remain open for review. Prefer exact encoded decision over digest for acceptance binding unless a measured reason is recorded.

## Design rules applied

- Integer labels start at 1 and increase; negative labels reserved for COSE key/header conventions.
- All maps are deterministic (RFC 8949 preferred encoding, lexicographic key order).
- Unknown labels rejected.
- Time fields are non-negative epoch milliseconds (int, >=0); upper bounds and skew policy still open under ADR 0006.
- Identifier strings are UTF-8 text with explicit byte-length maximums (not character counts).
- Public keys appear as full public COSE_Key maps (kty/crv/x/y) rather than raw SPKI.
- Nonce is exactly 32 bytes (bstr).
- Role and outcome are UTF-8 text enumerations with small maximum lengths.

## 1. Enrollment proof payload

Candidate media type: `application/vnd.familymesh.enrollment-proof+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Source parity |
|------:|------|-----------|------------------------|---------------|
| 1 | protocol-version | uint | exactly 2 | new explicit |
| 2 | artifact-type | uint | exactly 1 (enrollment-proof) | new explicit |
| 3 | request-id | tstr | 1..256 UTF-8 bytes | FMER1 request ID |
| 4 | family-id | tstr | 1..256 UTF-8 bytes | FMER1 family ID |
| 5 | applicant-key-id | bstr | exactly 32 bytes | v2 key ID |
| 6 | requested-role | tstr | "OWNER" / "ADMIN" / "MEMBER" (max 16 bytes) | FMER1 role |
| 7 | created-at | int | >= 0 (epoch ms) | FMER1 created |
| 8 | expires-at | int | > created-at | FMER1 expiry |
| 9 | nonce | bstr | exactly 32 bytes | FMER1 nonce |

Identity-creation time from the v1 transfer envelope is **not** included in the signed payload (matches current signed semantics). Public key travels in the COSE_Sign1 unprotected or as a separate authenticated structure; reviewers must decide placement.

## 2. Initial-owner trust payload

Candidate media type: `application/vnd.familymesh.initial-owner-trust+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Source parity |
|------:|------|-----------|------------------------|---------------|
| 1 | protocol-version | uint | exactly 2 | new |
| 2 | artifact-type | uint | exactly 2 (initial-owner-trust) | new |
| 3 | family-id | tstr | 1..256 UTF-8 bytes | FMOT1 |
| 4 | owner-member-id | tstr | 1..256 UTF-8 bytes | FMOT1 |
| 5 | owner-key-id | bstr | exactly 32 bytes | v2 key ID |
| 6 | established-at | int | >= 0 (epoch ms) | FMOT1 |
| 7 | owner-public-key | map | exact public COSE_Key (kty=2, crv=1, x, y) | FMOT1 public key |

Self-signature proves possession only; independent fingerprint confirmation remains mandatory.

## 3. Owner decision payload

Candidate media type: `application/vnd.familymesh.owner-decision+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Source parity |
|------:|------|-----------|------------------------|---------------|
| 1 | protocol-version | uint | exactly 2 | new |
| 2 | artifact-type | uint | exactly 3 (owner-decision) | new |
| 3 | family-id | tstr | 1..256 UTF-8 bytes | FMOD1 |
| 4 | request-id | tstr | 1..256 UTF-8 bytes | FMOD1 |
| 5 | applicant-key-id | bstr | exactly 32 bytes | FMOD1 |
| 6 | owner-key-id | bstr | exactly 32 bytes | FMOD1 |
| 7 | outcome | tstr | "APPROVED" / "REJECTED" (max 16 bytes) | FMOD1 |
| 8 | decided-at | int | >= 0 (epoch ms) | FMOD1 |

Finalization still requires exact match to a durable owner-issued artifact (ADR 0013) once that design is approved.

## 4. Applicant acceptance payload

Candidate media type: `application/vnd.familymesh.applicant-acceptance+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Source parity |
|------:|------|-----------|------------------------|---------------|
| 1 | protocol-version | uint | exactly 2 | new |
| 2 | artifact-type | uint | exactly 4 (applicant-acceptance) | new |
| 3 | exact-decision | bstr | canonical CBOR of the complete tagged COSE_Sign1 of the approved decision (or its exact encoded form) | FMJA1 exact decision |
| 4 | applicant-key-id | bstr | exactly 32 bytes | FMJA1 |
| 5 | applicant-public-key | map | exact public COSE_Key | FMJA1 public key |
| 6 | accepted-at | int | >= 0 (epoch ms) | FMJA1 |

Prefer embedding the exact verified decision bytes over a digest. If a digest is later chosen, specify algorithm, input bytes, and negative vectors before any approval.

## Open decisions that block freezing

1. Placement of the applicant public key in the enrollment-proof flow (signed payload vs. separate structure).
2. Whether identity-creation time must become signed content.
3. Exact byte-length limits after CBOR encoding analysis (current numbers are v1-derived upper bounds).
4. Time-policy upper bounds, skew, and clock-rollback handling (ADR 0006).
5. Acceptance binding: exact encoded decision versus digest (exact preferred for parity).
6. Low-S policy finalization and Keystore behavior verification.

## Next steps for reviewers

- Confirm or adjust the candidate labels and types.
- Generate complete positive vectors once labels are provisionally agreed.
- Expand the negative-test outline with schema-specific mutations (unknown label inside payload, wrong type, oversized string, etc.).
- Keep all implementation blocked until independent vectors, Keystore tests, and the remaining ADR gates are satisfied.

No production COSE parser, E2EE messaging, P2P, or Room v8 change is claimed or authorized by this proposal.
