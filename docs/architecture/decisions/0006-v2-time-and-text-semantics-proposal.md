# ADR 0006 — v2 time and text-field semantics (proposal)

- Status: PROPOSED — review required; not an accepted protocol decision
- Date: 2026-11-03
- Scope: temporal validity and text canonicalization for the four enrollment artifacts
- Depends on: ADR 0003, ADR 0004, ADR 0005

## Context

The v1 Kotlin implementation establishes some useful invariants, but does not
fully specify a cross-platform time or text profile. Assigning CBOR labels or
publishing final v2 vectors before resolving these semantics would freeze
accidental behavior into the wire protocol.

Source inspection covered the v1 signer/verifier, transfer codecs, acceptance
verifier, domain request models, and Room nonce store.

## Source-backed observations

1. All protocol timestamps are signed 64-bit epoch milliseconds and current
   constructors reject negative values.
2. Applicant proof verification requires the supplied current time to satisfy
   `createdAt <= now < expiresAt`. There is no explicit maximum request
   lifetime or clock-skew allowance in the proof model.
3. Applicant acceptance must not predate the owner decision, must be before
   the pending request expiry, and must not be later than the verifier's
   current time.
4. The owner decision verifier also checks the request/decision time and
   request validity; the exact policy must be preserved or deliberately
   revised as part of v2.
5. The nonce store records the received time and request expiry, and its
   replay scope is one local Room database.
6. Text values are converted with Kotlin UTF-8 encoding. Some model limits use
   UTF-16 `String.length`, while signing/codec limits use UTF-8 byte lengths.
7. Decoders reject malformed UTF-8, but encoders do not explicitly reject
   malformed UTF-16 input before conversion. A JVM encoder may replace
   unpaired surrogate code units, so distinct malformed in-memory strings
   need not have distinct encoded representations.
8. No Unicode normalization policy is specified. Applying NFC/NFKC silently
   would change identifiers and signed semantics.

## Proposed v2 rules for review

### T1 — Timestamp representation

Use CBOR unsigned integers for non-negative Unix epoch milliseconds, bounded
to the signed 64-bit range supported by the current Android domain model.
Reject negative values, values above the supported maximum, and arithmetic
overflow. Do not reinterpret timestamps as seconds or floating-point values.

### T2 — Request validity

Keep the half-open validity interval `createdAt <= now < expiresAt`.
Define a protocol-level maximum request lifetime and an explicit clock-skew
policy before finalizing the profile. These values must be product/security
decisions backed by offline-device and usability requirements, not guessed
during codec implementation.

Until those values are approved, v2 vectors must test interval boundaries
but must not claim a final maximum TTL or skew allowance.

### T3 — Distinguish signed event time from local receipt time

Treat these as separate concepts and fields:

- `createdAt`, `decidedAt`, and `acceptedAt` are signed event-time claims made by the respective device.
- `receivedAt` / `recordedAt` are local observer timestamps. They are not transferable protocol facts and must not be included in another device's signed artifact unless the protocol explicitly defines a signed receipt.
- Nonce consumption is performed at local proof receipt and remains installation-local in the current design.

### T4 — Decision and acceptance ordering

Preserve these minimum ordering constraints unless a reviewed lifecycle
decision supersedes them:

- owner decision is associated with the exact family and pending request;
- decision time cannot be earlier than request creation;
- approval/acceptance cannot be treated as valid after request expiry;
- acceptance cannot predate the approved owner decision;
- acceptance cannot claim a time later than the verifier's accepted current
  time, subject to any explicitly approved skew policy.

Specify whether rejected decisions share the same temporal rules as approvals.
Specify whether an acceptance must be produced before expiry or merely
received/verified before expiry; v1 currently constrains the signed
`acceptedAt` value, not a separately authenticated receipt time.

### S1 — Text encoding

For v2, encode text as UTF-8 with strict rejection of malformed Unicode
input before encoding. Decoders must use strict UTF-8 and reject malformed
sequences. Length limits are byte limits, not language-runtime character
counts.

### S2 — Unicode normalization

Do not normalize identifiers implicitly. The initial proposal is exact
Unicode scalar-value preservation with no NFC/NFKC transformation. If the
product requires normalization or ASCII-only identifiers, specify that as an
explicit identifier-profile rule and apply it consistently before signing,
comparison, storage, and display.

### S3 — Identifier profile

Before assigning final limits, inventory the actual generators and persisted
identifier formats for family IDs, request IDs, member IDs, and key IDs.
Then decide whether each identifier is:
- a restricted canonical ASCII token; or
- a UTF-8 string with a precisely defined scalar-value and byte-length cap.

Do not reuse v1's mixed character/byte limits as if they were one coherent
protocol rule.

## Required test cases before schema freeze

- timestamp zero, maximum supported timestamp, negative and overflow inputs;
- exact creation boundary, exact expiry boundary, and one unit on either side;
- decision before request creation and after expiry;
- acceptance before decision, exactly at expiry, and later than verifier time;
- malformed UTF-8 and unpaired UTF-16 surrogate input on the encoder path;
- canonically equivalent but byte-distinct Unicode strings, proving that no
  implicit normalization occurs;
- byte-limit boundaries for every text field;
- cross-language equality of the encoded text bytes and signed payload bytes.

## Explicit non-decisions

This ADR does not choose numeric TTL or skew values, does not define CBOR
labels, does not change v1 behavior, does not authorize a production v2
implementation, and does not alter replay scope, ownership, membership, or
authorization semantics.

## Acceptance gates

1. Product/security owner approves maximum request lifetime and skew policy.
2. Identifier generators and persisted formats are inventoried.
3. Decision/acceptance receipt-time semantics are explicitly approved.
4. Independent implementations reproduce the text and temporal negative
   vectors.
5. ADR 0003/0004/0005 are updated to reflect approved choices before final
   schema labels and artifact vectors are frozen.


## Receipt-time expiry decision record — review required

The two policies have materially different offline behavior and must not be
collapsed into a timestamp-validation detail.

### Option A — owner receipt deadline

The owner installation must receive and validate the applicant acceptance
before the request expiry instant. The verifier uses the owner's local
receipt time for this check; the applicant's signed `acceptedAt` remains a
separate event-time claim and must also satisfy the existing ordering rules.

Advantages:
- expiry has a clear operational meaning on the owner installation;
- delayed delivery cannot revive an expired request;
- cleanup and inbox eligibility can use one locally observable boundary.

Costs:
- an acceptance created before expiry but delivered after expiry is rejected;
- offline transport and intermittent connectivity can cause otherwise
  valid applicants to restart enrollment;
- a trustworthy local clock is still required, with an explicit skew and
  rollback policy.

### Option B — signed-event deadline

An acceptance whose signed `acceptedAt` is before expiry may be accepted
after expiry, subject to a separately specified maximum delivery delay and
retention period.

Advantages:
- tolerates delayed delivery and offline transfer after the applicant has
  completed the acceptance in time;
- aligns eligibility with the applicant's signed event rather than network
  availability.

Costs:
- the owner must retain pending request facts beyond nominal expiry;
- a signed timestamp does not prove when the artifact was actually created;
  a compromised applicant clock/key can backdate a claim unless additional
  controls exist;
- a delivery-delay bound and cleanup policy are mandatory, not optional.

### Required product/security inputs

Before selecting either option, product/security review must answer:

1. Must a household be able to complete enrollment when devices are offline
   from one another for a prolonged period?
2. Is expiry intended to stop *new acceptance creation* or to stop *owner
   authorization after a deadline*?
3. What maximum offline delivery delay, if any, is acceptable?
4. What clock rollback and clock-skew threat model is in scope?
5. How long must the owner retain pending request facts and signed decisions?

No numeric TTL, skew allowance, delivery grace period, or expiry cleanup
behavior is selected by this record. Until these inputs are approved, do not
add an `EXPIRED` state, change v1 behavior, or implement a v2 policy based
on an assumed default.

### Implementation invariant after selection

Whichever policy is approved, preserve three distinct values:
applicant-signed `acceptedAt`, owner-local `receivedAt`, and owner-local
`recordedAt`. The verifier must receive the local receipt timestamp from a
trusted owner-side boundary; callers must not be able to supply an arbitrary
receipt time as if it were authenticated.
