# COSE v2 low-S policy and signature conversion review notes

- Status: **Documentation support for protocol review — not approved for implementation or production use**
- Profile: draft ADR 0003
- Related: [negative-test outline](cose-v2-negative-test-outline.md), primitive KATs 0001–0004
- Date: 2026-11-28

This note records the exact conversion steps, rejection criteria, and high-S construction guidance that reviewers need before any parser or verifier is written. It does **not** implement conversion logic, does **not** add executable tests, and does **not** approve the draft profile or the low-S policy.

## Conversion requirements (proposal)

Android/JCA `SHA256withECDSA` produces ASN.1 DER signatures. The proposed COSE ESP256 profile requires a fixed-width 64-byte `R || S`.

Strict conversion steps (must fail closed on any deviation):

1. Parse the DER SEQUENCE of exactly two INTEGER values (r, s). Reject extra fields, indefinite lengths, or trailing data after the SEQUENCE.
2. Reject negative integers, zero values, or values >= the P-256 order n.
3. Reject overlong encodings (leading zero bytes that are not required for positive interpretation).
4. Left-pad each accepted scalar to exactly 32 bytes (unsigned big-endian).
5. Concatenate the two 32-byte values into a 64-byte signature.
6. On output (signing), optionally normalize S to `min(s, n-s)` per the low-S proposal.
7. On input (verification), reject any signature that is not exactly 64 bytes, or whose S >= n/2 under the low-S proposal, or whose R or S is outside `1 <= value < n`.

The low-S policy is a FamilyMesh proposal for canonical representation. It is **not** required by RFC 9053 or RFC 9864 and must not be described as an IETF mandate. Android Keystore may not expose deterministic nonce control (RFC 6979); do not claim RFC 6979 compliance without device-level evidence.

## High-S construction from existing KATs

All primitive KATs already publish a low-S value. A high-S test vector is obtained by replacing that S with `n - s` (where n is the P-256 order) while keeping R unchanged, then reassembling the COSE object. Verification under the proposed policy must reject the resulting object before or during cryptographic check.

Example (from KAT 0001 low-S):

- Low-S: `193ed0dfc2fa72851f01b6ee6fecc32bc9cabde4537b13a721fd76caab338a4b`
- High-S is the modular complement (reviewers can compute `n - s` with any big-integer library). The resulting 64-byte signature must be rejected under the low-S rule.

The same construction applies to the published low-S values in KATs 0002–0004.

## Kid mismatch concrete example (from KAT 0001)

Original kid (32 bytes) in the protected header:
```
aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6e
```

Mutated (flip the last byte `6e` → `6f`):
```
aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6f
```

A future parser that accepts the mutated protected header with the original COSE_Key must reject on key-ID binding. The same rule applies to every KAT.

## Additional rejection cases for conversion

- DER with an extra trailing byte after the SEQUENCE
- INTEGER with a leading zero that makes the encoding non-minimal
- r or s equal to zero or equal to n
- Signature bstr of length other than 64 (already covered in the negative-test outline)
- High-S as described above

These notes accelerate review of the signature-representation gate in ADR 0003. They do not constitute vectors, a parser, or approval.

Room schema remains v7. Authorization remains fail-closed. No production COSE, E2EE, or network transport is claimed or implemented.
