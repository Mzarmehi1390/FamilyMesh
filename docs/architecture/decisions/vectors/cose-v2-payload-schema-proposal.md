# COSE v2 payload schema proposal (review draft)

- Status: **Documentation proposal only — not approved for implementation or production use**
- Profile: draft ADR 0003
- Source inventory: [ADR 0005](../0005-v1-enrollment-source-field-inventory.md)
- Parity notes: [ADR 0004](../0004-cose-v2-schema-parity-review.md)
- Date: 2026-11-28

This document proposes candidate integer labels, CBOR types, and strict byte limits for the four enrollment artifact payloads so that reviewers can discuss concrete schemas before any parser or vector work. It does **not** freeze the schemas, does **not** implement a parser, and does **not** authorize a wire migration.

All proposals preserve the fail-closed semantics of the current v1 codecs. Unknown labels, duplicate keys, non-deterministic encodings, and missing required fields must be rejected. Time fields remain epoch milliseconds (`uint` or negative-integer-free `int`) with no upper bound yet defined (ADR 0006 still open). Public keys are proposed as nested deterministic COSE_Key maps (labels 1, -1, -2, -3 only).

## Design choices recorded for review

1. **Exact encoded decision binding** is preferred for applicant acceptance (parity with current v1 FMJA1). A digest alternative is not selected.
2. **Identity creation time** of the applicant proof remains transfer metadata and is **not** placed in the signed payload (matching current v1).
3. Identifier limits are expressed solely in UTF-8 bytes. Empty strings and over-limit values are rejected.
4. Signature material is never part of the payload map; it is the COSE signature field (exactly 64-byte R||S after low-S normalization).
5. Protocol version and artifact type are required inside every payload for dual binding with external AAD and content type.

## 1. Enrollment proof payload

Candidate media type: `application/vnd.familymesh.enrollment-proof+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Required |
|------:|------|-----------|------------------------|----------|
| 1 | protocol-version | uint | exactly 2 | yes |
| 2 | artifact-type | uint | exactly 1 (enrollment-proof) | yes |
| 3 | request-id | tstr | 1..256 UTF-8 bytes, non-blank | yes |
| 4 | family-id | tstr | 1..256 UTF-8 bytes, non-blank | yes |
| 5 | applicant-key-id | bstr | exactly 32 bytes (v2 key ID) | yes |
| 6 | requested-role | tstr | one of "OWNER", "ADMIN", "MEMBER" (1..16 bytes) | yes |
| 7 | created-at-ms | int | >= 0 | yes |
| 8 | expires-at-ms | int | > created-at-ms | yes |
| 9 | nonce | bstr | exactly 32 bytes | yes |

No other labels permitted.

## 2. Initial-owner trust payload

Candidate media type: `application/vnd.familymesh.initial-owner-trust+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Required |
|------:|------|-----------|------------------------|----------|
| 1 | protocol-version | uint | exactly 2 | yes |
| 2 | artifact-type | uint | exactly 2 (initial-owner-trust) | yes |
| 3 | family-id | tstr | 1..256 UTF-8 bytes | yes |
| 4 | owner-member-id | tstr | 1..256 UTF-8 bytes | yes |
| 5 | owner-key-id | bstr | exactly 32 bytes | yes |
| 6 | established-at-ms | int | >= 0 | yes |
| 7 | owner-public-key | map | deterministic COSE_Key (EC2/P-256 only) | yes |

## 3. Owner decision payload

Candidate media type: `application/vnd.familymesh.owner-decision+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Required |
|------:|------|-----------|------------------------|----------|
| 1 | protocol-version | uint | exactly 2 | yes |
| 2 | artifact-type | uint | exactly 3 (owner-decision) | yes |
| 3 | family-id | tstr | 1..256 UTF-8 bytes | yes |
| 4 | request-id | tstr | 1..256 UTF-8 bytes | yes |
| 5 | applicant-key-id | bstr | exactly 32 bytes | yes |
| 6 | owner-key-id | bstr | exactly 32 bytes | yes |
| 7 | outcome | tstr | "APPROVED" or "REJECTED" | yes |
| 8 | decided-at-ms | int | >= 0 | yes |

## 4. Applicant acceptance payload

Candidate media type: `application/vnd.familymesh.applicant-acceptance+cbor`

| Label | Name | CBOR type | Constraints (proposal) | Required |
|------:|------|-----------|------------------------|----------|
| 1 | protocol-version | uint | exactly 2 | yes |
| 2 | artifact-type | uint | exactly 4 (applicant-acceptance) | yes |
| 3 | exact-owner-decision | bstr | canonical re-encoding of the approved owner-decision COSE_Sign1, 1..4096 bytes | yes |
| 4 | applicant-key-id | bstr | exactly 32 bytes | yes |
| 5 | applicant-public-key | map | deterministic COSE_Key (EC2/P-256 only) | yes |
| 6 | accepted-at-ms | int | >= 0 | yes |

The nested owner decision must itself verify under the owner-decision profile before the acceptance signature is accepted.

## Open review items (not resolved by this proposal)

- Final numeric labels and media-type strings.
- Whether identity-creation time should ever become signed.
- Exact-decision vs digest binding (exact preferred here).
- Time policy, maximum lifetime, and clock-skew (ADR 0006).
- Maximum overall COSE object size after CBOR encoding analysis.
- Independent second-implementation vectors for complete (non-primitive) payloads.

This proposal exists solely to accelerate protocol review. Room schema remains v7. Authorization remains fail-closed. No production COSE, E2EE, or network transport is claimed or implemented.
