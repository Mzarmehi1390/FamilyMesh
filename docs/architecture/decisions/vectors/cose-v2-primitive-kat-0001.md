# COSE v2 primitive known-answer vector 0001

- Status: **Test fixture only; not a production protocol approval**
- Profile under test: draft ADR 0003
- Artifact context: enrollment proof
- Purpose: independently verify deterministic CBOR, COSE protected-header
  serialization, key-ID derivation, Sig_structure construction, P-256 ECDSA,
  low-S normalization, and tagged COSE_Sign1 assembly.

## Security warning

The private scalar for this fixture is the public constant `1`. It is
intentionally known to everyone and MUST NEVER be used for a real device,
test installation, demo identity, or production signature. This fixture
provides no secret key material. It exists solely for reproducible tests.

The payload below is a minimal cryptographic fixture, not the final
EnrollmentProof schema. Artifact payload labels and required semantic fields
remain subject to ADR 0003 review.

## Fixed inputs

- Curve: NIST P-256 / secp256r1
- Private scalar (fixture only): `01` left-padded to 32 bytes
- Public point: standard P-256 generator point (G)
- Hash: SHA-256
- COSE alg: ESP256 (`-9`)
- Deterministic test signing nonce: RFC 6979 with HMAC-SHA-256
- Signature normalization: `s = min(s, n-s)`
- Protected content type:
  `application/vnd.familymesh.enrollment-proof+cbor`
- External AAD: ASCII `FamilyMesh-EnrollmentProof-v2` followed by one
  zero byte

## Encoded values (hex)

All hex strings are lowercase, contain no separators, and represent bytes.

### Public COSE_Key

Map labels: `1=2` (EC2), `-1=1` (P-256), `-2=x`, `-3=y`.

```
a4010220012158206b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c2962258204fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5
```

### Key identifier

Formula:
`SHA-256(ASCII("FamilyMesh-KeyID-v2") || 00 || deterministic-CBOR(COSE_Key))`

```
aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6e
```

### Minimal fixture payload

CBOR map `{1: 2, 2: 1, 3: h'6b6174'}`.

```
a30102020103436b6174
```

### External AAD

```
46616d696c794d6573682d456e726f6c6c6d656e7450726f6f662d763200
```

### Protected header map (encoded bytes carried in the COSE bstr)

The map contains `1=-9` (alg), `3=<content type>`, and `4=<32-byte kid>`.

```
a301280378306170706c69636174696f6e2f766e642e66616d696c796d6573682e656e726f6c6c6d656e742d70726f6f662b63626f72045820aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6e
```

### Sig_structure

The exact CBOR encoding of:
`["Signature1", protected-header-bytes, external-AAD, payload-bytes]`

```
846a5369676e6174757265315859a301280378306170706c69636174696f6e2f766e642e66616d696c796d6573682e656e726f6c6c6d656e742d70726f6f662b63626f72045820aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6e581e46616d696c794d6573682d456e726f6c6c6d656e7450726f6f662d7632004aa30102020103436b6174
```

SHA-256 of Sig_structure:

```
7807045142d0adad944d0b694eb886957ccf8c3514a5d019e2ee12c94d2835eb
```

### ECDSA signature

Fixed-width 32-byte unsigned big-endian scalars:

- R:
  `ce1b5c8f1d4df03943493d10dbfb7732bcd63557d9d8eca5f77e7b30cfd645f4`
- S (low-S):
  `193ed0dfc2fa72851f01b6ee6fecc32bc9cabde4537b13a721fd76caab338a4b`

COSE signature (`R || S`, exactly 64 bytes):

```
ce1b5c8f1d4df03943493d10dbfb7732bcd63557d9d8eca5f77e7b30cfd645f4193ed0dfc2fa72851f01b6ee6fecc32bc9cabde4537b13a721fd76caab338a4b
```

### Complete tagged COSE_Sign1

CBOR tag 18 followed by `[protected, {}, payload, signature]`:

```
d2845859a301280378306170706c69636174696f6e2f766e642e66616d696c796d6573682e656e726f6c6c6d656e742d70726f6f662b63626f72045820aa3ea3d23cf530a1f41c88d53c066ea6b2f183932e852ffe74da31c6a8554c6ea04aa30102020103436b61745840ce1b5c8f1d4df03943493d10dbfb7732bcd63557d9d8eca5f77e7b30cfd645f4193ed0dfc2fa72851f01b6ee6fecc32bc9cabde4537b13a721fd76caab338a4b
```

## Verification performed for this fixture

- Public point is the P-256 generator corresponding to private scalar 1.
- Signature was generated from the encoded Sig_structure using RFC 6979
  HMAC-SHA-256 nonce derivation and ECDSA over P-256.
- S was normalized to the low half of the P-256 scalar range.
- The resulting DER-form ECDSA signature successfully verified against the
  exact Sig_structure using an independent cryptographic library.
- Encodings were generated with a small deterministic-CBOR encoder and
  checked byte-for-byte while assembling this fixture.

This is a reproducible primitive vector, not yet independent
cross-implementation interoperability evidence. Before implementation
approval, another implementation must reproduce the key ID, Sig_structure,
signature, and complete COSE object from the stated inputs.

## Required negative tests derived from this vector

1. Flip one bit in payload, protected header, AAD, or signature: verification
   must fail.
2. Change external AAD to another artifact context: verification must fail.
3. Change alg from ESP256 (-9) to ES256 (-7): reject before verification.
4. Change tag 18 to an untagged array: reject under this profile.
5. Change signature length from 64 bytes: reject before JCA verification.
6. Replace low-S with `n-s`: reject under the proposed low-S policy.
7. Change kid while retaining the key: reject key-ID binding.
8. Encode any map key, integer, or length non-deterministically: reject.
9. Add an unknown protected or COSE_Key label: reject under the closed draft.
10. Substitute a non-P-256 point or malformed coordinate: reject.

## Remaining work

This single primitive fixture does not close the ADR 0003 gates. Still
required: four complete artifact payload schemas and vectors, malformed CBOR
corpus, independent second implementation, Android Keystore instrumentation,
Room transaction/migration tests, and explicit approval of replay, time,
owner-authority, revocation, and recovery policies.
