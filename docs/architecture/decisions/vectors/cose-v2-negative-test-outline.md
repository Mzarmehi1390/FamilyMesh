# COSE v2 negative-test outline (derived from primitive KAT 0001)

- Status: **Documentation support for protocol review; not a production approval or complete corpus**
- Profile: draft ADR 0003
- Base fixture: [COSE v2 primitive KAT 0001](cose-v2-primitive-kat-0001.md)
- Purpose: record concrete malformed and cross-binding cases that any future parser must reject before implementation approval.

This outline does not implement a parser, does not add executable tests, and does not satisfy the independent cross-implementation or full-schema gates in ADR 0003. It is a checklist of expected fail-closed behavior.

## Security warning

The private scalar in the base KAT is the public constant 1. Never use it outside test-vector verification.

## Required rejection cases

All cases start from the exact bytes of KAT 0001 and apply one change. Verification or parsing must fail closed (reject, do not fall back).

1. **Payload bit flip**: Invert one bit in the payload CBOR (`a30102020103436b6174`). Signature verification must fail.
2. **Protected header bit flip**: Invert one bit in the protected header bstr. Signature verification must fail.
3. **External AAD change**: Replace the AAD with `FamilyMesh-OwnerDecision-v2\0`. Signature verification must fail (cross-artifact binding).
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

## Remaining gates

These cases support review only. Still required before any implementation approval:
- Complete payload schemas and positive vectors for all four artifacts.
- Independent second CBOR/COSE implementation reproducing the vectors.
- Android Keystore instrumentation.
- Explicit approval of time, replay, owner-authority, revocation, and recovery policies (ADR 0006 and related).
- Room v8 remains blocked until ADR 0006/0008/0012 gates are satisfied.

No production COSE parser, E2EE, or network transport is claimed or implemented.
