# ADR 0003: Draft COSE v2 enrollment profile

- Status: **Draft proposal — not approved for implementation or production use**
- Date: 2026-11-03
- Scope: signed device-enrollment proof, initial-owner trust statement,
  owner decision, and applicant acceptance

## 1. Goals and non-goals

The v2 profile should replace the custom fixed-order binary signed payloads
with a standards-based, independently implementable representation. It must
retain the existing trust boundaries: a self-signed initial-owner statement
is not trusted until its fingerprint is independently confirmed; an owner
decision does not itself grant production authorization; local authenticated
device rows remain installation-local until a separate membership and
synchronization design is approved.

This proposal does not define message encryption, group key management,
transport, multi-owner consensus, distributed revocation, recovery, or a
production membership resolver.

## 2. Proposed cryptographic suite

- Device authentication key: NIST P-256 / secp256r1, ECDSA.
- Hash: SHA-256.
- COSE algorithm: ESP256, integer value -9, as defined by RFC 9864.
- Reject polymorphic COSE ES256 (-7); RFC 9864 marks it deprecated.
- Keep the private key in Android Keystore and use the existing typed key
  lifecycle. Do not export private key material.
- Do not reuse this signing key for ECDH or message encryption.

The current Android/JCA `SHA256withECDSA` implementation emits ASN.1 DER
signatures. COSE ESP256 carries fixed-width `R || S`: exactly 32 bytes for
R followed by exactly 32 bytes for S. Conversion must strictly parse DER,
reject negative, zero, out-of-range, truncated, overlong, and trailing
values, and left-pad each valid scalar to 32 bytes. Verification must reject
any COSE signature not exactly 64 bytes and enforce `1 <= r,s < n`.

### Low-S policy proposal

For one canonical signature representation, the proposed profile is:
normalize outgoing S to `min(s, n-s)` and reject high-S signatures on input.
This policy requires explicit test vectors and review against the Android
Keystore/JCA integration before approval. It is not a requirement stated by
RFC 9053 or RFC 9864 and must not be described as an IETF requirement.

RFC 9053 recommends deterministic ECDSA where feasible. Android Keystore
controls the signing operation and may not expose RFC 6979 deterministic
nonce control. Do not claim RFC 6979 compliance unless verified on the actual
supported providers/devices. The security and interoperability review must
document this platform limitation.

## 3. Proposed public-key representation

Use a public-only COSE EC2 key map with exactly these entries:

| Label | Name | Required value |
|---:|---|---|
| 1 | kty | 2 (EC2) |
| -1 | crv | 1 (P-256) |
| -2 | x | 32-byte unsigned big-endian coordinate |
| -3 | y | 32-byte unsigned big-endian coordinate |

Reject private key label -4, unknown labels, duplicate labels, wrong CBOR
types, non-32-byte coordinates, coordinates outside the field, and points
that are not valid members of P-256. Do not accept compressed-y boolean
encoding in this profile. Public key import must validate the point and
reconstruct the same P-256 identity as the source SPKI key.

The key-ID proposal is SHA-256 over:

`ASCII("FamilyMesh-KeyID-v2") || 0x00 || deterministic-CBOR(public-key-map)`

The hashed map contains only labels 1, -1, -2, and -3 above; it excludes
kid, alg, key_ops, and all private material to avoid circular or contextual
identifiers. The resulting key ID is exactly 32 bytes. This changes the
existing v1 SPKI-based key ID and therefore requires an explicit versioned
migration; never silently reinterpret a v1 identifier as v2.

## 4. Proposed signed artifact

Represent each artifact as a tagged COSE_Sign1 object (CBOR tag 18) with:

- protected header map containing:
  - alg (label 1): -9 (ESP256)
  - content type (label 3): the exact artifact-specific media type
  - kid (label 4): 32-byte v2 key identifier of the signer
- unprotected header: empty map
- payload: a definite-length deterministic-CBOR byte string containing the
  artifact-specific payload map
- signature: 64-byte fixed-width P-256 R || S

The exact media types and payload schemas must be frozen in the final
specification, one per artifact. Candidate media types are:

- `application/vnd.familymesh.enrollment-proof+cbor`
- `application/vnd.familymesh.initial-owner-trust+cbor`
- `application/vnd.familymesh.owner-decision+cbor`
- `application/vnd.familymesh.applicant-acceptance+cbor`

These names are proposals pending review. Unknown media types, algorithms,
critical parameters, and unsupported protocol versions must fail closed.
Do not put security-relevant algorithm identifiers only in unprotected
headers. Do not allow tagged/untagged alternatives: this profile requires
tag 18.

## 5. Deterministic CBOR and signature context

Use RFC 8949 core deterministic encoding requirements for all protocol CBOR:
preferred serialization, definite lengths only, and bytewise lexicographic
ordering of deterministic map-key encodings. Reject duplicate map keys,
indefinite-length items, non-preferred integer/length encodings, trailing
bytes, and unsupported CBOR types.

For COSE_Sign1, construct the RFC 9052 Sig_structure with context
`Signature1`, the exact serialized protected-header byte string, and an
artifact-specific external AAD. Proposed AAD values are the ASCII byte
strings below, including the terminating zero byte:

- `FamilyMesh-EnrollmentProof-v2\0`
- `FamilyMesh-InitialOwnerTrust-v2\0`
- `FamilyMesh-OwnerDecision-v2\0`
- `FamilyMesh-ApplicantAcceptance-v2\0`

The payload must also carry its explicit protocol version and artifact type.
This dual binding prevents an artifact signed for one workflow from being
accepted in another. The exact AAD bytes must be frozen in published vectors.

## 6. Payload schema requirements

Every artifact payload schema must explicitly define integer labels, CBOR
types, maximum lengths, required/optional fields, and unknown-field behavior.
At minimum, preserve the current semantic bindings:

- Enrollment proof: protocol version, artifact type, request ID, family ID,
  applicant key ID, requested role, creation/expiry times, and 32-byte nonce.
- Initial-owner trust: protocol version, family ID, owner member ID, owner
  key ID, establishment time, and public COSE key.
- Owner decision: protocol version, family ID, request ID, applicant key ID,
  owner key ID, outcome, and decision time.
- Applicant acceptance: protocol version, exact approved decision digest or
  exact encoded decision, applicant key ID, applicant public COSE key, and
  acceptance time. The public key is present in the current v1 acceptance
  artifact and must not be dropped without an explicit reviewed decision.

All identifiers need specified byte/text encodings and strict maximum sizes.
Time fields need a single specified unit and range. No field may be silently
defaulted when absent. Unknown payload labels should be rejected in v2 unless
a later version explicitly defines extension behavior.

See also [ADR 0004: COSE v2 schema parity review](0004-cose-v2-schema-parity-review.md)
for the v1-to-v2 semantic parity gap and the schema-freeze checklist.

## 7. Migration and compatibility

- Keep v1 and v2 parsers separate and selected by an unambiguous outer
  version/envelope discriminator.
- Never retry a failed v2 parse as v1, or vice versa.
- Never accept a v1 signature under v2 rules or a v2 signature under v1 rules.
- Keep existing v1 artifacts readable only for a documented migration window;
  the final policy must define whether migration is one-way and whether
  re-enrollment is required.
- Existing v1 key IDs are SPKI-based. A v2 key ID is derived from the
  deterministic COSE public-key map. Store and label versions explicitly.
- No migration may create a trusted owner pin or authenticated-device row
  without the existing independent fingerprint and signature checks.

## 8. Required test and review gates

A first primitive known-answer fixture is now recorded at
[COSE v2 primitive KAT 0001](vectors/cose-v2-primitive-kat-0001.md). It covers
one enrollment-context key, key ID, protected header, external AAD,
Sig_structure, deterministic test signature, low-S normalization, and final
tagged COSE_Sign1 bytes. Its public test key is deliberately insecure and
must never be used outside test-vector verification. This fixture is not a
complete artifact schema or independent interoperability sign-off.

Before implementation is approved:

1. Publish independent known-answer vectors for all four artifacts:
   payload bytes, protected-header bytes, external AAD, Sig_structure bytes,
   public COSE key, key ID, DER input signature, normalized R || S, and final
   COSE_Sign1 bytes.
2. Include malformed vectors: duplicate keys, reordered/non-deterministic
   maps, non-shortest integer encodings, indefinite lengths, unknown labels,
   wrong curve, invalid point, wrong coordinate width, wrong kid, wrong alg,
   wrong content type, wrong tag, invalid DER, zero/out-of-range scalars,
   high-S, short/long signature, trailing bytes, and cross-artifact replay.
3. Run cross-implementation vectors in at least two independent CBOR/COSE
   implementations.
4. Add Android API 31+ Keystore instrumentation tests for key generation,
   public-key export/import, signing, verification, key invalidation, and
   actual security-level reporting.
5. Add Room instrumentation tests for transaction rollback, unique nonce
   enforcement, expiry cleanup, and migration behavior.
6. Threat-model clock rollback, app-data restoration, device backup/restore,
   local database modification, offline owners, and multi-device replay.
7. Resolve maximum proof lifetime, clock-skew tolerance, replay retention,
   owner authority lifecycle, revocation, and recovery before enabling
   authorization or network transport.

## 9. References

- RFC 9052 — COSE structures, COSE_Sign1, and Sig_structure.
- RFC 9053 — COSE EC2 keys and ECDSA fixed-width R || S signatures.
- RFC 8949 — CBOR core deterministic encoding requirements.
- RFC 9864 — fully specified ESP256 (-9); deprecation of COSE ES256 (-7).

## 10. Decision status

This document is an engineering proposal, not an accepted cryptographic
standard or security approval. Do not implement a breaking wire migration
until the payload labels, content types, external AAD, low-S policy, key-ID
migration, time policy, replay semantics, and trust lifecycle have received
explicit review and are covered by reproducible independent vectors.
