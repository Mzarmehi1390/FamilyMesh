# ADR 0001: Device authentication signature profile

- Status: **Proposed for implementation review**
- Date: 2026-11-03
- Scope: device identity and membership-authentication signatures only

## Context

FamilyMesh needs a stable device public identity that can authenticate enrollment
requests and later membership-control messages. Android Keystore supports EC
key-pair generation and ECDSA signing, including an Android-documented NIST
P-256 (secp256r1) profile. The IETF COSE specifications define interoperable
representations for EC2/P-256 keys and ECDSA signatures. RFC 9864 adds the
fully specified COSE identifier ESP256 for ECDSA with P-256 and SHA-256, and
deprecates the polymorphic COSE ES256 identifier.

## Proposed decision

1. Use a dedicated **NIST P-256 (secp256r1) ECDSA signing key** for device
   authentication.
2. Use **SHA-256 with ECDSA**. In COSE messages, identify this exact profile
   with **ESP256** (COSE algorithm value -9), rather than the deprecated
   polymorphic COSE ES256 identifier (-7).
3. Generate and retain the private signing key in Android Keystore. Never
   export or serialize private key material.
4. Represent the public signing key on the protocol boundary as a COSE EC2
   key using curve P-256. Validate key type, curve, algorithm, and operation
   before accepting a key or signature.
5. Keep signing identity separate from encryption/key-agreement identities.
   Do not reuse the signing key for ECDH or message encryption.
6. Keep hardware-backed status as an observed property, not a universal
   assumption. A device may provide software-backed Keystore; the app must
   not claim StrongBox or TEE protection unless it has verified the security
   level.

## Rationale

- Android documents generating P-256 EC key pairs in Android Keystore and
  using ECDSA with SHA-256.
- COSE provides a compact, standardized EC2 key representation and signature
  structures suitable for exchanging device identities between peers.
- RFC 9864 defines ESP256 as a fully specified P-256/SHA-256 signature
  algorithm and deprecates polymorphic COSE ES256.
- Separate keys and explicit algorithm identifiers reduce accidental
  cross-protocol key reuse and algorithm-confusion risks.

## Required implementation details

- Define a versioned, deterministic COSE_Key encoding and strict parser.
- Define a stable key identifier over the deterministic public-key
  representation, including domain separation and versioning.
- Convert Android/JCA ECDSA signature encoding to and from the COSE signature
  representation with strict length and scalar-range checks.
- Bind signed enrollment and authorization messages to protocol version,
  family-space ID, device-key ID, action, nonce, and relevant membership
  version; reject unknown versions and replayed nonces.
- Specify user-presence/authentication policy separately from cryptographic
  key generation. Do not silently require biometrics or assume all devices
  have the same authentication capabilities.
- Map Keystore failures and key invalidation to the existing typed lifecycle
  errors. Never silently generate a replacement identity.
- Add known-answer tests, malformed-key/signature tests, cross-device
  interoperability tests, and Android API 31+ instrumentation tests.
- Verify the actual security level where supported and expose it accurately.

## Explicit non-goals

This decision does not choose a group messaging protocol, end-to-end message
encryption scheme, ECDH key-agreement profile, invitation-token format,
initial-owner ceremony, recovery mechanism, or synchronization consistency
model. Those remain separate decisions and must not be inferred from the
signature profile.

## Review gates

This proposal is not authorization to ship cryptographic enrollment. Before
implementation is treated as production security, review canonical encoding,
key identifiers, signature conversion, replay protection, initial-owner trust,
device revocation, and recovery as one end-to-end protocol. Update this ADR if
platform compatibility testing reveals unsupported behavior.

## References

- RFC 9052 — COSE structures and signing process.
- RFC 9053 — COSE initial algorithms and ECDSA signature representation.
- RFC 8949 — CBOR core deterministic encoding requirements.
- RFC 9864 — fully specified COSE algorithms, including ESP256 (-9).
