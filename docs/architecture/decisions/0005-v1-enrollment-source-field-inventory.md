# ADR 0005: v1 enrollment source field inventory

- Status: **Source-backed inventory; v2 schema still not approved**
- Date: 2026-11-03
- Inspected revision: main at `01144664ed9512552f266cd02f61905862298250`
- Sources: `core/identity/.../SignedDeviceEnrollmentRequest.kt`,
  `EnrollmentProofTextCodec.kt`, `InitialOwnerTrustBundle.kt`,
  `EnrollmentDecision.kt`, `EnrollmentAcceptance.kt`, and
  `DevicePublicIdentity.kt`.

## Scope and interpretation

This inventory separates signed semantic content from transfer-envelope
metadata. The current v1 format is a custom binary format; these observations
must not be mistaken for final CBOR labels or types. Length limits below
reflect the current Kotlin validation/codec behavior, not a recommended v2
limit. Most string checks combine a character-count limit in constructors
with a UTF-8 byte-count limit in encoders; v2 must define one unambiguous rule.

All timestamps named below are Unix epoch milliseconds represented as
non-negative signed 64-bit integers in the current Kotlin models. No upper
bound, clock-skew policy, or maximum lifetime is established by these models.

## 1. Applicant enrollment proof (FMER1)

### Signed semantic content

| Field | Current Kotlin source | Current constraint / encoding |
|---|---|---|
| request ID | `requestId: String` | Nonblank; constructor length <= 256 characters; signing encoder UTF-8 length 1..256 bytes |
| family ID | `familySpaceId: String` | Same as request ID |
| applicant key ID | `deviceKeyId: String` | Nonblank; constructor length <= 128 characters; signing encoder UTF-8 length <= 256 bytes (identity key-ID max is 128 characters) |
| requested role | `FamilyRole` | Enum strings: OWNER, ADMIN, MEMBER; encoded as nonblank UTF-8, max 16 bytes |
| created time | `createdAtEpochMillis: Long` | >= 0; signed as 8-byte big-endian integer |
| expiry time | `expiresAtEpochMillis: Long` | > created time; signed as 8-byte big-endian integer |
| nonce | `ByteArray` | Exactly 32 bytes |
| signature | `ByteArray` | 1..256 bytes; JCA ECDSA DER in current implementation, not part of signed payload |

### Transfer-envelope-only identity metadata

FMER1 also carries:
- identity creation time: non-negative `Long`, 8-byte big-endian;
- public key: 1..1024 bytes (currently X.509 SubjectPublicKeyInfo);
- transfer signature bytes: the proof signature, 1..256 bytes.

The key ID and public key are checked for consistency during encoding and
later proof verification. The identity creation time is not included in the
proof's signing payload. The complete decoded envelope is bounded to 4096
payload bytes and 6000 text characters.

## 2. Initial-owner trust bundle (FMOT1)

| Field | Current Kotlin source | Current constraint / encoding |
|---|---|---|
| family ID | `familySpaceId: String` | Nonblank; <=256 characters; encoded UTF-8 length 1..256 bytes |
| owner member ID | `ownerMemberId: String` | Same as family ID |
| owner device key ID | `ownerDeviceKeyId: String` | Nonblank; <=128 characters; encoded UTF-8 length <=128 bytes |
| establishment time | `establishedAtEpochMillis: Long` | >=0; 8-byte big-endian |
| owner public key | `publicKeyBytes: ByteArray` | 1..1024 bytes; current representation X.509 SPKI |
| signature | `ByteArray` | 1..256 bytes; DER ECDSA |

The text envelope has a 2048-byte payload cap and 3000-character text cap.
The signature covers the family ID, owner member ID, owner key ID,
establishment time, and public-key bytes. The self-signature proves
possession only; independent fingerprint confirmation remains required.

## 3. Owner decision (FMOD1)

| Field | Current Kotlin source | Current constraint / encoding |
|---|---|---|
| family ID | `familySpaceId: String` | Nonblank; <=256 characters and UTF-8 <=256 bytes |
| request ID | `requestId: String` | Same as family ID |
| applicant key ID | `applicantDeviceKeyId: String` | Nonblank; <=128 characters and UTF-8 <=128 bytes |
| owner key ID | `ownerDeviceKeyId: String` | Same as applicant key ID |
| outcome | `EnrollmentDecisionOutcome` | APPROVED or REJECTED; UTF-8 enum string max 16 bytes |
| decision time | `decidedAtEpochMillis: Long` | >=0; 8-byte big-endian |
| signature | `ByteArray` | 1..256 bytes; DER ECDSA |

The signature covers all semantic fields except the signature itself. The
decision envelope is bounded to 2048 payload bytes and 3000 text characters.
Verification additionally binds family ID, request ID, applicant key ID, and
owner key ID to the exact pending request and locally trusted owner.

## 4. Applicant acceptance (FMJA1)

| Field | Current Kotlin source | Current constraint / encoding |
|---|---|---|
| exact owner decision | `decision: SignedEnrollmentDecision` | Nested `FMOD1.` encoded text; nonblank UTF-8 length <=3000 bytes |
| applicant key ID | `applicantDeviceKeyId: String` | Nonblank; <=128 characters; UTF-8 <=128 bytes |
| applicant public key | `applicantPublicKeyBytes: ByteArray` | 1..512 bytes; current representation X.509 SPKI |
| acceptance time | `acceptedAtEpochMillis: Long` | >=0; 8-byte big-endian |
| signature | `ByteArray` | 1..256 bytes; DER ECDSA |

The acceptance signature covers the exact nested decision text, applicant key
ID, applicant public-key bytes, and acceptance time. Verification requires an
approved decision, matches the applicant key ID to the pending request,
checks the public-key fingerprint, and verifies both owner and applicant
signatures. The transfer envelope is bounded to 4096 payload bytes and 6000
text characters.

## Cross-artifact observations and unresolved v2 decisions

1. **Encoding limits are inconsistent.** The proof request/family IDs use a
   256-byte UTF-8 bound; the owner trust and decision IDs use 256 bytes; key
   IDs are 128 bytes; acceptance embeds a 3000-byte encoded decision.
2. **Character and byte limits differ.** Some constructors check Kotlin
   `String.length`, while encoders check UTF-8 byte length. The v2 profile
   must define limits in bytes and reject invalid Unicode consistently.
3. **Public-key encoding is not yet protocol-neutral.** v1 uses X.509 SPKI;
   v2 proposes COSE_Key. The conversion and fingerprint migration need
   explicit vectors.
4. **The proof's identity creation time is transfer metadata**, not part of
   the proof's signed semantic fields. Decide whether v2 keeps it in the
   signed payload, in a separate authenticated structure, or drops it.
5. **Acceptance binds exact decision text**, not a digest. A v2 digest option
   changes semantics and requires a deliberate decision; exact encoded
   decision bytes are the closest parity-preserving choice.
6. **Signature size caps are transport/model caps**, not the COSE signature
   length. v2 ESP256 signatures must be exactly 64-byte R||S after DER
   conversion; do not carry over the v1 256-byte DER cap.
7. **No time upper bounds exist.** Define maximum lifetime, future skew,
   overflow behavior, and clock rollback handling before v2 approval.
8. **Payload-size caps are envelope-specific.** Recalculate them for CBOR
   and the selected public-key/signature representation; do not copy the v1
   caps without an encoded-size analysis.

## Next gate

This inventory now provides source-backed semantics and limits for the four
artifacts. Before assigning v2 integer labels, review whether the proof's
identity-creation timestamp is required, settle exact-decision versus digest
binding, and define a single byte-oriented identifier policy and time policy.
Then freeze the v2 payload schemas and generate complete positive and negative
vectors. No runtime or wire migration is authorized by this inventory.
