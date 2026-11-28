# COSE v2 negative-test outline (derived from primitive KATs 0001–0004)

- Status: **Documentation support for protocol review; not a production approval or complete corpus**
- Profile: draft ADR 0003
- Base fixtures: [KAT 0001](cose-v2-primitive-kat-0001.md), [KAT 0002](cose-v2-primitive-kat-0002.md), [KAT 0003](cose-v2-primitive-kat-0003.md), [KAT 0004](cose-v2-primitive-kat-0004.md)
- Related proposal: [COSE v2 payload schema proposal](cose-v2-payload-schema-proposal.md) (not approved)
- Related notes: [low-S and signature conversion review](cose-v2-low-s-signature-conversion-review.md) (not approved)
- Purpose: record concrete malformed and cross-binding cases that any future parser must reject before implementation approval.

This outline does not implement a parser, does not add executable tests, and does not satisfy the independent cross-implementation or full-schema gates in ADR 0003. It is a checklist of expected fail-closed behavior.

## Security warning

The private scalar in the base KATs is the public constant 1. Never use it outside test-vector verification.

## Required rejection cases

All cases start from the exact bytes of any of the four primitive KATs (enrollment-proof, owner-decision, applicant-acceptance, or initial-owner-trust contexts) and apply one change. Verification or parsing must fail closed (reject, do not fall back). Using one vector's AAD or content type with another's bytes must fail (cross-artifact binding).

1. **Payload bit flip**: Invert one bit in the payload CBOR (`a30102020103436b6174`). Signature verification must fail.
2. **Protected header bit flip**: Invert one bit in the protected header bstr. Signature verification must fail.
3. **External AAD change**: Replace the AAD with another artifact's AAD. Signature verification must fail (cross-artifact binding).
4. **Algorithm substitution**: Change alg from -9 (ESP256) to -7 (ES256) in the protected header. Reject before cryptographic verification.
5. **Missing tag**: Present the COSE array without CBOR tag 18. Reject under this profile (tagged-only).
6. **Signature length**: Change the signature bstr length from 64 to 63 or 65. Reject before JCA verification.
7. **High-S**: Replace the low-S value with `n - s`. Reject under the proposed low-S policy. See the [low-S conversion review notes](cose-v2-low-s-signature-conversion-review.md) for construction guidance and concrete complements for all four KATs.
8. **Kid mismatch**: Change one byte of the kid in the protected header while keeping the same COSE_Key. Reject key-ID binding.
9. **Non-deterministic map**: Reorder map keys or use non-preferred integer encoding in any map. Reject under deterministic-CBOR rules.
10. **Unknown label**: Add an unrecognized label to the protected header or COSE_Key map. Reject under the closed draft profile.
11. **Invalid point**: Replace a coordinate with a value outside the field or a non-point. Reject on public-key import.
12. **Wrong curve**: Change crv from 1 (P-256) to another value. Reject.
13. **Trailing bytes**: Append extra data after the complete tagged COSE_Sign1. Reject.
14. **Indefinite length**: Replace a definite-length bstr or array with indefinite encoding. Reject.
15. **Duplicate keys**: Encode a map with the same key twice. Reject.

## Concrete mutation examples (from KAT 0001)

These examples illustrate specific cases from the checklist using the exact fixture bytes of [KAT 0001](cose-v2-primitive-kat-0001.md). They are documentation only; no parser is exercised here. Reviewers can regenerate the full mutated objects with a deterministic CBOR/COSE encoder and confirm rejection.

### 1. Payload bit flip

Original minimal payload CBOR:
```
a30102020103436b6174
```

Mutated (last byte `74` → `75`, one-bit change in the bstr content):
```
a30102020103436b6175
```

Any future verifier must reject the resulting tagged COSE_Sign1 (same protected header, AAD, and signature) because the Sig_structure no longer matches.

### 2. Protected header bit flip

Original protected-header bstr begins:
```
a301280378306170706c69636174696f6e2f766e642e66616d696c796d6573682e656e726f6c6c6d656e742d70726f6f662b63626f72045820aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6e
```

Mutated (invert one bit in the first content-type character byte, e.g. first `61` of "application" → `60`):
```
a301280378306160706c69636174696f6e2f766e642e66616d696c796d6573682e656e726f6c6c6d656e742d70726f6f662b63626f72045820aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6e
```

Signature verification must fail because the protected-header bytes that enter the Sig_structure have changed.

### 3. External AAD cross-binding

Original AAD for enrollment-proof (KAT 0001):
```
46616d696c794d6573682d456e726f6c6c6d656e7450726f6f662d763200
```

Substitute AAD from owner-decision (KAT 0002):
```
46616d696c794d6573682d4f776e65724465636973696f6e2d763200
```

Signature verification on the original KAT 0001 object with the substituted AAD must fail. The same rule applies in the opposite direction and across all four contexts.

### 4. Algorithm substitution

Original protected-header map encoding starts with the deterministic bytes for label 1 = -9 (ESP256):
```
a30128
```

Mutated (replace the CBOR integer encoding of -9 with the encoding of -7, producing a different protected-header bstr that a future parser must reject before any cryptographic verification):
```
a30126
```

(The full protected-header bstr and resulting COSE object change accordingly. The profile requires rejection of alg ≠ -9.)

### 5. Missing tag

Original complete object begins with CBOR tag 18 (`d284...`). Presenting the same array without the tag (i.e. an untagged COSE_Sign1 array) must be rejected under this profile. The profile is tagged-only; untagged alternatives are not accepted.

### 6. Signature length

Original COSE signature (exactly 64 bytes):
```
ce1b5c8f1d4df03943493d10dbfb7732bcd63557d9d8eca5f77e7b30cfd645f4193ed0dfc2fa72851f01b6ee6fecc32bc9cabde4537b13a721fd76caab338a4b
```

Mutated (truncate to 63 bytes by dropping the final byte):
```
ce1b5c8f1d4df03943493d10dbfb7732bcd63557d9d8eca5f77e7b30cfd645f4193ed0dfc2fa72851f01b6ee6fecc32bc9cabde4537b13a721fd76caab338a4
```

A future parser must reject the object before any JCA signature verification because the signature bstr is not exactly 64 bytes. The same rejection applies to a 65-byte extension.

### 7. High-S

Original low-S from KAT 0001:
```
193ed0dfc2fa72851f01b6ee6fecc32bc9cabde4537b13a721fd76caab338a4b
```

High-S (`n - s`):
```
e6c12f1f3d058d7be0fe491190133cd3f31c3cc9539c8addd1bc53f8512f9b06
```

Replacing the low-S in the 64-byte signature while keeping R unchanged produces an object that a future parser must reject under the proposed low-S policy. Full concrete complements for all four KATs (0001–0004) are recorded in the [low-S conversion review notes](cose-v2-low-s-signature-conversion-review.md).

### 8. Kid mismatch

Original kid (last 32 bytes of the protected-header bstr content):
```
aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6e
```

Mutated (final byte `6e` → `6f`):
```
aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6f
```

A future parser that accepts the mutated protected header together with the original COSE_Key must reject on key-ID binding. Construction details are also recorded in the [low-S conversion review notes](cose-v2-low-s-signature-conversion-review.md).

### 9. Non-deterministic map

Original protected-header map uses bytewise lexicographic key ordering (labels 1, 3, 4). Reordering the map keys (for example encoding label 4 before label 1) produces a non-deterministic CBOR encoding that must be rejected under the draft deterministic-CBOR rules, even if the semantic content is identical.

### 10. Unknown label

Adding an unrecognized label (for example label 99 with a zero-length bstr) to the protected-header map or to the COSE_Key map produces a closed-profile violation. A future parser must reject the object. The same rule applies to unknown payload labels once schemas are frozen.

### 11. Invalid point

Original public COSE_Key (from KAT 0001):
```
a4010220012158206b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c2962258204fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5
```

Mutated (first byte of the x-coordinate `6b` → `6c`, producing a value that is not a valid P-256 point):
```
a4010220012158206c17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c2962258204fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5
```

A future parser must reject this key on public-key import (point validation fails). The same rejection applies to any coordinate that lies outside the field or does not satisfy the curve equation.

### 12. Wrong curve

Original COSE_Key encodes crv = 1 (P-256) as the byte sequence `2001` (label -1 followed by unsigned integer 1).

Mutated (change the curve value to 2):
```
a4010220022158206b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c2962258204fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5
```

A future parser must reject the object because the profile admits only crv = 1. The same rule applies to any other curve identifier.

### 13. Trailing bytes

Original complete tagged COSE_Sign1 (KAT 0001) ends with the 64-byte signature. Appending a single extra byte (for example `00`) after the complete object must cause the parser to reject the input. The same applies to any non-empty trailing data.

### 14. Indefinite length

Original protected-header bstr uses definite-length encoding (`58 59` followed by 89 content bytes). Replacing any definite-length bstr or array with the indefinite-length encoding (for example starting a bstr with `5f` and terminating with `ff`) must be rejected under the draft deterministic-CBOR rules. A future parser must fail closed on the first indefinite-length item.

### 15. Duplicate keys

Original protected-header map encodes each label exactly once. Encoding the same map with label 1 appearing twice (even with identical values) produces a duplicate-key violation. A future parser must reject the object under the closed deterministic-CBOR profile. The same rule applies to any map in the COSE_Key or payload.

These examples make the checklist more actionable for reviewers without adding executable tests or approving the draft profile.

## Lessons from current v1 fail-closed boundaries

The existing v1 enrollment codecs and parsers already enforce several fail-closed rules that the v2 profile must preserve or strengthen. These are documented here so reviewers can map them to the cases above without claiming that v1 is the production wire format.

- **Canonical unpadded Base64URL only**: `CanonicalBase64Url` rejects padding, non-alphabet characters, empty/whitespace input, and non-canonical re-encodings. v2 deterministic CBOR must similarly reject non-preferred encodings and trailing data.
- **P-256-only public keys**: `P256PublicKeyParser` rejects non-P-256 curves (P-384/P-521), truncated/malformed SPKI, empty bytes, and non-EC algorithm identifiers. v2 must reject wrong `crv`, invalid points, and non-32-byte coordinates on import.
- **Bounded transfer envelopes**: Existing text codecs reject malformed prefixes, oversized inputs, and trailing bytes after the decoded payload. v2 tagged COSE_Sign1 must reject untagged arrays, wrong tags, and any trailing bytes.
- **Exact signature binding**: Verifiers reject tampered signatures and require both owner and applicant signatures on finalization. v2 must reject signature length mismatches, high-S values, and any alteration of the protected header or external AAD.

These mappings support the review checklist only. They do not constitute additional executable tests or approval of the draft profile.

## Remaining gates

These cases support review only. Still required before any implementation approval:
- Complete payload schemas and positive vectors for all four artifacts (primitive KATs now cover all four contexts; a review proposal for labels/types is now available, including a size analysis and encoding sketch).
- Independent second CBOR/COSE implementation reproducing the vectors.
- Android Keystore instrumentation.
- Explicit approval of time, replay, owner-authority, revocation, and recovery policies (ADR 0006 and related).
- Room v8 remains blocked until ADR 0006/0008/0012 gates are satisfied.

No production COSE parser, E2EE, or network transport is claimed or implemented.
