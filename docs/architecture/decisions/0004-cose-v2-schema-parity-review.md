# ADR 0004: COSE v2 schema parity review

- Status: **Review note — no wire schema approved**
- Date: 2026-11-03
- Depends on: ADR 0002 and draft ADR 0003

## Purpose

Record the semantic parity check between the existing v1 artifacts and the
v2 requirements before assigning final CBOR labels or implementing a parser.
This note deliberately does not invent identifier encodings, timestamp units,
maximum lengths, or optionality that have not yet been verified against the
source model and validators.

## V1-to-v2 semantic parity matrix

| Artifact | Existing v1 semantic content (ADR 0002) | Required v2 parity |
|---|---|---|
| Applicant proof (FMER1) | request ID, family ID, applicant key ID, requested role, creation and expiry times, 32-byte nonce, domain/version | Preserve all fields; additionally bind explicit v2 protocol version and artifact type in the payload |
| Initial-owner trust (FMOT1) | family ID, owner member ID, owner key ID, establishment time, public key, domain/version | Preserve the public key as a public COSE_Key and preserve all identity/time fields |
| Owner decision (FMOD1) | family ID, request ID, applicant key ID, owner key ID, outcome, decision time, domain/version | Preserve all fields and bind the decision to the owner key and exact pending request |
| Applicant acceptance (FMJA1) | exact encoded owner decision, applicant key ID, applicant public key, acceptance time, domain/version | Preserve the exact decision binding, applicant key ID, applicant public COSE_Key, and acceptance time |

## Concrete gap found in draft ADR 0003

The initial v2 requirements in ADR 0003 section 6 mention the applicant
acceptance's exact approved decision digest or encoded decision, applicant
key ID, and acceptance time, but omit the **applicant public key** carried by
the current v1 acceptance artifact. The omission is not evidence that the
key is unnecessary. It must be explicitly reviewed and either retained or
removed through a documented security/design decision.

Likewise, the owner-decision schema must retain the exact request/applicant/
owner bindings; the initial-owner statement must retain its public key; and
the applicant proof must retain its nonce and validity interval. No field may
silently disappear during v1-to-v2 migration.

## Schema freeze checklist

Before finalizing any integer labels or publishing full artifact vectors,
inspect the Kotlin source models, codecs, validators, persistence consumers,
and tests, then record for every field:

1. Stable integer label and whether it is required.
2. Exact CBOR type and exact text/byte encoding.
3. Maximum encoded and decoded length.
4. Numeric range, including timestamp unit, epoch, and accepted bounds.
5. Whether empty values are valid.
6. Unknown-field and duplicate-field behavior.
7. Whether the field is covered by the signature and any external context.
8. Compatibility behavior when reading v1 versus v2.

Do not infer these values from field names or documentation summaries alone.
The schema must match actual validated v1 semantics unless a separately
reviewed change explicitly alters them.

## Acceptance binding decision still open

The v2 proposal currently allows either a digest of the approved decision or
the exact encoded decision. These are not interchangeable without defining
the digest algorithm, canonical input bytes, and the exact decision object
being hashed. Prefer retaining the exact encoded, verified decision unless
there is a measured size or lifecycle reason to use a digest. If a digest is
chosen, specify all of those details and publish positive and negative
vectors before approval.

## Migration and authorization guardrails

- Keep v1 and v2 decoders separate; never fall back across versions after a
  parse or signature failure.
- Do not create or pin trust based only on a self-signed initial-owner
  statement.
- Do not let a valid COSE artifact alone create production authorization.
- Do not change transport, Room schema, or runtime trust behavior as part of
  this documentation review.
- Treat every unresolved field mapping as a release blocker for v2.

## Next evidence required

The source-backed inventory is now recorded in [ADR 0005](0005-v1-enrollment-source-field-inventory.md). It confirms the applicant public key in acceptance and surfaces additional parity decisions: proof identity-creation time is transfer metadata rather than signed content; acceptance currently binds the exact encoded decision; constructor character limits differ from UTF-8 byte limits; and all timestamps are epoch milliseconds without upper bounds. Review these choices before freezing CBOR labels. Independent second-implementation verification remains mandatory.


## 2026-11-27 source revalidation — exact v1 signing and verification boundaries

Inspected the four Kotlin artifact models/codecs and their key-ID/Base64URL
helpers on the PR #2 integration branch. This is a source revalidation, not
a protocol approval.

| Artifact | Transfer envelope | Signed semantic fields | Important non-signed or external context |
|---|---|---|---|
| Applicant proof | `FMER1.` | Domain/version, request ID, family ID, applicant key ID, requested role, created/expiry epoch milliseconds, 32-byte nonce | Public key and identity-created timestamp are carried in the transfer envelope; the key bytes are checked against the key ID, but identity-created time is not part of the proof signing payload |
| Initial-owner trust | `FMOT1.` | Domain/version, family ID, owner member ID, owner key ID, establishment epoch milliseconds, owner public-key bytes | Self-signature establishes possession only; external fingerprint confirmation is still required to trust the owner |
| Owner decision | `FMOD1.` | Domain/version, family ID, request ID, applicant key ID, owner key ID, APPROVED/REJECTED outcome, decision epoch milliseconds | Verification additionally binds all request/owner identifiers to the exact pending request and locally pinned owner |
| Applicant acceptance | `FMJA1.` | Domain/version, exact encoded `FMOD1.` text, applicant key ID, applicant public-key bytes, acceptance epoch milliseconds | Verification requires an approved decision and validates both owner and applicant signatures |

All four use custom fixed-order, length-prefixed binary signing payloads,
not CBOR. Text envelopes use versioned prefixes, bounded fields, and
canonical unpadded Base64URL. Signatures are produced/verified through JCA
`SHA256withECDSA`; v1 stores provider DER signature bytes. The current v1
code does not define COSE fixed-width `R || S`, low-S normalization, or a
low-S rejection rule. Those remain v2 design gates, not implicit properties
of the existing implementation.

The key ID is currently lowercase hexadecimal SHA-256 over
`UTF8("FamilyMesh:device-key:v1\\0") || X.509 SubjectPublicKeyInfo bytes`.
It is not the draft v2 deterministic-CBOR COSE_Key identifier. The proof's
identity-created timestamp is transfer metadata and is not covered by the
applicant proof signature; do not silently treat it as an authenticated
event-time claim.

All current event timestamps are non-negative signed 64-bit epoch
milliseconds. The source defines no maximum request lifetime or clock-skew
allowance. Current checks enforce a half-open proof interval
`createdAt <= now < expiresAt`; owner decision time is constrained to the
request interval and not later than the verifier's current time; acceptance
time cannot predate the decision, exceed verifier time, or reach request
expiry. The acceptance verifier checks the signed acceptance time against
expiry, not a separately authenticated owner receipt timestamp. This is the
existing signed-event-time behavior; it must not be confused with a selected
future owner-receipt deadline.

The source review confirms the already recorded persistence gates remain
distinct from wire-schema parity: owner FMOD1 issuance is not durably
ledgered, the owner inbox state does not transition on issuance, and the
finalized-row replay fast path does not identify the complete FMJA1 artifact.
See ADR 0007, 0008, 0011, 0012, and 0013. These issues must be solved through
the reviewed owner-ledger/replay design, not by changing artifact fields
speculatively.

No integer CBOR labels, final media types, TTL/skew values, low-S policy,
owner-authority rule, replay-retention rule, or Room v8 migration is approved
by this revalidation. No runtime, cryptography, wire format, or database
change was made.
