# COSE v2 negative-test outline (derived from primitive KATs 0001, 0002, and 0003)

- Status: **Documentation support for protocol review; not a production approval or complete corpus**
- Profile: draft ADR 0003
- Base fixtures: [COSE v2 primitive KAT 0001](cose-v2-primitive-kat-0001.md), [COSE v2 primitive KAT 0002](cose-v2-primitive-kat-0002.md), [COSE v2 primitive KAT 0003](cose-v2-primitive-kat-0003.md)
- Purpose: record concrete malformed and cross-binding cases that any future parser must reject before implementation approval.

This outline does not implement a parser, does not add executable tests, and does not satisfy the independent cross-implementation or full-schema gates in ADR 0003. It is a checklist of expected fail-closed behavior.

## Security warning

The private scalar in the base KATs is the public constant 1. Never use it outside test-vector verification.

## Required rejection cases

All cases start from the exact bytes of KAT 0001 (enrollment-proof context), KAT 0002 (owner-decision context), or KAT 0003 (applicant-acceptance context) and apply one change. Verification or parsing must fail closed (reject, do not fall back). Using one vector's AAD or content type with another's bytes must fail (cross-artifact binding).

1. **Payload bit flip**: Invert one bit in the payload CBOR (`a30102020103436b6174`). Signature verification must fail.
2. **Protected header bit flip**: Invert one bit in the protected header bstr. Signature verification must fail.
3. **External AAD change**: Replace the AAD with another artifact's AAD. Signature verification must fail (cross-artifact binding).
4. **Algorithm substitution**: Change alg from -9 (ESP256) to -7 (ES256) in the protected header. Reject before cryptographic verification.
5. **Missing tag**: Present the COSE array without CBOR tag 18. Reject under this profile (tagged-only).
6. **Signature length**: Change the signature bstr length from 64 to 63 or 65. Reject before JCA verification.
7. **High-S**: Replace the low-S value with `n - s`. Reject under the proposed low-S policy.
8. **Kid mismatch**: Change one byte of the kid in the protected header while keeping the same COSE_Key. Reject key-ID binding.
9. **Non-deterministic map**: Reorder map keys or use non-preferred integer encoding in any map. Reject under deterministic-CBOR rules.
10. **Unknown label**: Add an unrecognized label to the protected header or COSE_Key map. Reject under the closed draft profile.
11. **Invalid point**: Replace a coordinate with a value outside the field or a non-point. Reject on public-key import.
12. **Wrong curve**: Change crv from 1 (P-256) to another value. Reject.
13. **Trailing bytes**: Append extra data after the complete tagged COSE_Sign1. Reject.
14. **Indefinite length**: Replace a definite-length bstr or array with indefinite encoding. Reject.
15. **Duplicate keys**: Encode a map with the same key twice. Reject.

## Lessons from current v1 fail-closed boundaries

The existing v1 enrollment codecs and parsers already enforce several fail-closed rules that the v2 profile must preserve or strengthen. These are documented here so reviewers can map them to the cases above without claiming that v1 is the production wire format.

- **Canonical unpadded Base64URL only**: `CanonicalBase64Url` rejects padding, non-alphabet characters, empty/whitespace input, and non-canonical re-encodings. v2 deterministic CBOR must similarly reject non-preferred encodings and trailing data.
- **P-256-only public keys**: `P256PublicKeyParser` rejects non-P-256 curves (P-384/P-521), truncated/malformed SPKI, empty bytes, and non-EC algorithm identifiers. v2 must reject wrong `crv`, invalid points, and non-32-byte coordinates on import.
- **Bounded transfer envelopes**: Existing text codecs reject malformed prefixes, oversized inputs, and trailing bytes after the decoded payload. v2 tagged COSE_Sign1 must reject untagged arrays, wrong tags, and any trailing bytes.
- **Exact signature binding**: Verifiers reject tampered signatures and require both owner and applicant signatures on finalization. v2 must reject signature length mismatches, high-S values, and any alteration of the protected header or external AAD.

These mappings support the review checklist only. They do not constitute additional executable tests or approval of the draft profile.

## Remaining gates

These cases support review only. Still required before any implementation approval:
- Complete payload schemas and positive vectors for all four artifacts (one remaining: initial-owner trust).
- Independent second CBOR/COSE implementation reproducing the vectors.
- Android Keystore instrumentation.
- Explicit approval of time, replay, owner-authority, revocation, and recovery policies (ADR 0006 and related).
- Room v8 remains blocked until ADR 0006/0008/0012 gates are satisfied.

No production COSE parser, E2EE, or network transport is claimed or implemented.
