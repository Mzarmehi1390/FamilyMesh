# ADR 0002: Enrollment wire-protocol audit

- Status: **Audit recorded; production protocol decision pending**
- Date: 2026-11-03
- Scope: local enrollment proof, initial-owner trust, owner decision, and applicant acceptance

## Current wire artifacts

| Envelope | Purpose | Signed content |
|---|---|---|
| `FMER1.` | Applicant enrollment proof | Domain/version, request ID, family ID, device key ID, requested role, creation/expiry times, 32-byte nonce |
| `FMOT1.` | Initial-owner trust bundle | Domain/version, family ID, owner member ID, owner key ID, establishment time, public key |
| `FMOD1.` | Owner decision | Domain/version, family ID, request ID, applicant key ID, owner key ID, outcome, decision time |
| `FMJA1.` | Applicant acceptance | Domain/version, encoded exact owner decision, applicant key ID, applicant public key, acceptance time |

These are custom fixed-order, length-prefixed v1 binary payloads with
domain-separated signing bytes and bounded text envelopes. They are not
CBOR, COSE_Key, or COSE_Sign1 and must not be represented as interoperable
or production-approved wire formats.

## Verified hardening now in source

- Bounded decoders use strict UTF-8 and reject trailing payload bytes.
- Text transfer codecs enforce canonical unpadded Base64URL.
- Public-key verification accepts only P-256 EC SubjectPublicKeyInfo keys.
- Key ID is currently a versioned, domain-separated SHA-256 fingerprint of
  the X.509 SubjectPublicKeyInfo bytes.
- Android signing uses JCA `SHA256withECDSA`, yielding provider DER signatures.

## Decisions still required

1. **Canonical public key:** select canonical COSE EC2/P-256 encoding and
   specify exact required/forbidden labels and byte lengths.
2. **Key ID:** define a versioned domain-separated digest over the canonical
   protocol key representation and migration from the current SPKI fingerprint.
3. **Signature representation:** define strict DER parsing and fixed-width
   COSE R||S conversion, scalar range checks, and whether low-S normalization
   or rejection is mandatory.
4. **Signed structures:** define whether to use COSE_Sign1 with external AAD
   and specify exact content type, algorithm identifier, critical headers,
   and context separation for every artifact.
5. **Time policy:** define maximum proof lifetime, clock-skew tolerance,
   timestamp source assumptions, and behavior when a device clock moves.
6. **Replay/freshness:** retain nonce uniqueness across the intended trust
   domain; define expiry cleanup, persistent audit retention, and behavior
   when several owner devices operate offline.
7. **Authority lifecycle:** define one/multiple owner semantics, initial-owner
   ceremony, ownership transfer, revocation propagation, and recovery.
8. **Interoperability:** publish known-answer vectors for every signed
   structure and malformed-input vectors for independent implementations.

## Current replay behavior

A 32-byte random applicant nonce is domain-hashed and inserted under a
unique primary key in Room in the same transaction that inserts the pending
request. Expired nonce rows are deleted when a new proof is consumed. This
prevents repeat consumption in one intact local database during the request
validity window; it is not cross-device or distributed replay protection.

## Trust and authorization boundary

The owner trust bundle is self-signed. A recipient must compare its complete
fingerprint with the owner through an independent trusted channel before
pinning. Pins are immutable per family on that installation. Owner decisions
are verified against the local pin and exact pending request. Finalization
creates an owner-local authenticated-device row after verifying both
signatures. No row is synchronized to peers and no production membership
resolver consumes these rows. Authorization remains fail-closed.

## Decision

Do not begin network transport or wire local authenticated-device records
into production authorization until the open decisions above are reviewed,
documented, and covered by cross-implementation test vectors and Android
instrumentation tests. Keep ADR 0001 proposed; current P-256 enforcement is
a defensive implementation constraint, not acceptance of the complete COSE
proposal.
