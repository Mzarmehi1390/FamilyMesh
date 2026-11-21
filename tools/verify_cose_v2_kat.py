#!/usr/bin/env python3
"""Independently verify FamilyMesh COSE v2 primitive KAT 0001.

Uses only Python's standard library for deterministic CBOR/DER construction
and the OpenSSL CLI for ECDSA verification. This validates the documented
primitive fixture only; it is not a production COSE implementation.
"""
from __future__ import annotations

import hashlib
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

VECTOR = (
    Path(__file__).resolve().parents[1]
    / "docs/architecture/decisions/vectors/cose-v2-primitive-kat-0001.md"
)

P256_G_X = bytes.fromhex("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296")
P256_G_Y = bytes.fromhex("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5")
P256_ORDER = int("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16)
MEDIA_TYPE = "application/vnd.familymesh.enrollment-proof+cbor"
AAD = b"FamilyMesh-EnrollmentProof-v2\x00"


def head(major: int, n: int) -> bytes:
    if n < 0:
        raise ValueError("negative CBOR argument")
    if n < 24:
        return bytes([(major << 5) | n])
    if n <= 0xFF:
        return bytes([(major << 5) | 24, n])
    if n <= 0xFFFF:
        return bytes([(major << 5) | 25]) + n.to_bytes(2, "big")
    if n <= 0xFFFFFFFF:
        return bytes([(major << 5) | 26]) + n.to_bytes(4, "big")
    return bytes([(major << 5) | 27]) + n.to_bytes(8, "big")


def cbor(value) -> bytes:
    if isinstance(value, bool):
        return b"\xf5" if value else b"\xf4"
    if isinstance(value, int):
        return head(0, value) if value >= 0 else head(1, -1 - value)
    if isinstance(value, bytes):
        return head(2, len(value)) + value
    if isinstance(value, str):
        raw = value.encode("utf-8")
        return head(3, len(raw)) + raw
    if isinstance(value, list):
        return head(4, len(value)) + b"".join(cbor(item) for item in value)
    if isinstance(value, dict):
        pairs = [(cbor(k), cbor(v)) for k, v in value.items()]
        pairs.sort(key=lambda pair: pair[0])  # RFC 8949 core deterministic bytewise order
        return head(5, len(pairs)) + b"".join(k + v for k, v in pairs)
    raise TypeError(type(value))


def section(document: str, heading: str) -> str:
    marker = f"### {heading}"
    start = document.index(marker)
    rest = document[start + len(marker):]
    next_heading = re.search(r"\n### |\n## ", rest)
    return rest[: next_heading.start()] if next_heading else rest


def hex_blocks(text: str) -> list[bytes]:
    blocks = re.findall(r"```\s*\n([0-9a-fA-F]+)\s*\n```", text)
    return [bytes.fromhex(block) for block in blocks]


def der_len(n: int) -> bytes:
    if n < 128:
        return bytes([n])
    raw = n.to_bytes((n.bit_length() + 7) // 8, "big")
    return bytes([0x80 | len(raw)]) + raw


def der(tag: int, body: bytes) -> bytes:
    return bytes([tag]) + der_len(len(body)) + body


def der_integer(raw: bytes) -> bytes:
    value = raw.lstrip(b"\x00") or b"\x00"
    if value[0] & 0x80:
        value = b"\x00" + value
    return der(0x02, value)


def raw_ecdsa_to_der(signature: bytes) -> bytes:
    if len(signature) != 64:
        raise AssertionError("COSE ESP256 signature must be exactly 64 bytes")
    r, s = signature[:32], signature[32:]
    return der(0x30, der_integer(r) + der_integer(s))


def spki_p256(x: bytes, y: bytes) -> bytes:
    # SubjectPublicKeyInfo( id-ecPublicKey, prime256v1, BIT STRING(04 || X || Y) )
    algorithm = der(
        0x30,
        bytes.fromhex("06072a8648ce3d0201")
        + bytes.fromhex("06082a8648ce3d030107"),
    )
    point = b"\x04" + x + y
    bit_string = der(0x03, b"\x00" + point)
    return der(0x30, algorithm + bit_string)


def verify_with_openssl(public_der: bytes, message: bytes, signature_der: bytes) -> bool:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        pub = root / "public.der"
        msg = root / "sig-structure.bin"
        sig = root / "signature.der"
        pub.write_bytes(public_der)
        msg.write_bytes(message)
        sig.write_bytes(signature_der)
        pem = subprocess.run(
            [
                "openssl", "pkey", "-pubin", "-inform", "DER", "-in", str(pub),
                "-pubout", "-out", str(root / "public.pem"),
            ],
            capture_output=True, text=True,
        )
        if pem.returncode:
            raise RuntimeError(pem.stderr)
        result = subprocess.run(
            [
                "openssl", "dgst", "-sha256", "-verify", str(root / "public.pem"),
                "-signature", str(sig), str(msg),
            ],
            capture_output=True, text=True,
        )
        return result.returncode == 0 and "Verified OK" in result.stdout


def main() -> int:
    if shutil.which("openssl") is None:
        print("ERROR: OpenSSL CLI is required for the independent signature check", file=sys.stderr)
        return 2
    document = VECTOR.read_text(encoding="utf-8")
    expected_public = hex_blocks(section(document, "Public COSE_Key"))[0]
    expected_kid = hex_blocks(section(document, "Key identifier"))[0]
    expected_payload = hex_blocks(section(document, "Minimal fixture payload"))[0]
    expected_aad = hex_blocks(section(document, "External AAD"))[0]
    protected_heading = "Protected header map (encoded bytes carried in the COSE bstr)"
    expected_protected = hex_blocks(section(document, protected_heading))[0]
    sig_section = section(document, "Sig_structure")
    sig_blocks = hex_blocks(sig_section)
    expected_sig_structure = sig_blocks[0]
    expected_sig_hash = sig_blocks[1]
    signature_section = section(document, "ECDSA signature")
    expected_signature = hex_blocks(signature_section)[0]
    expected_cose = hex_blocks(section(document, "Complete tagged COSE_Sign1"))[0]

    public_key = cbor({1: 2, -1: 1, -2: P256_G_X, -3: P256_G_Y})
    kid = hashlib.sha256(b"FamilyMesh-KeyID-v2\x00" + public_key).digest()
    payload = cbor({1: 2, 2: 1, 3: b"kat"})
    protected = cbor({1: -9, 3: MEDIA_TYPE, 4: kid})
    sig_structure = cbor(["Signature1", protected, AAD, payload])
    signature = expected_signature
    if len(signature) != 64:
        raise AssertionError("COSE ESP256 signature must be exactly 64 bytes")
    r_scalar = int.from_bytes(signature[:32], "big")
    s_scalar = int.from_bytes(signature[32:], "big")
    if not (1 <= r_scalar < P256_ORDER and 1 <= s_scalar < P256_ORDER):
        raise AssertionError("ECDSA signature scalar is outside the P-256 range")
    if s_scalar > P256_ORDER // 2:
        raise AssertionError("fixture signature is not low-S as documented")
    cose_sign1 = cbor([protected, {}, payload, signature])
    cose_sign1 = b"\xd2" + cose_sign1  # CBOR tag 18

    checks = {
        "public COSE_Key": (public_key, expected_public),
        "key identifier": (kid, expected_kid),
        "payload": (payload, expected_payload),
        "external AAD": (AAD, expected_aad),
        "protected header": (protected, expected_protected),
        "Sig_structure": (sig_structure, expected_sig_structure),
        "Sig_structure SHA-256": (hashlib.sha256(sig_structure).digest(), expected_sig_hash),
        "complete tagged COSE_Sign1": (cose_sign1, expected_cose),
    }
    for label, (actual, expected) in checks.items():
        if actual != expected:
            print(f"FAIL: {label}\n expected={expected.hex()}\n actual  ={actual.hex()}", file=sys.stderr)
            return 1
        print(f"PASS: {label}")

    public_der = spki_p256(P256_G_X, P256_G_Y)
    signature_der = raw_ecdsa_to_der(signature)
    if not verify_with_openssl(public_der, sig_structure, signature_der):
        print("FAIL: OpenSSL rejected the documented ESP256 signature", file=sys.stderr)
        return 1
    print("PASS: OpenSSL P-256 ECDSA/SHA-256 signature verification")

    if verify_with_openssl(public_der, cbor(["Signature1", protected, AAD + b"x", payload]), signature_der):
        print("FAIL: altered external AAD was accepted", file=sys.stderr)
        return 1
    if verify_with_openssl(public_der, cbor(["Signature1", protected, AAD, payload + b"\x00"]), signature_der):
        print("FAIL: altered payload was accepted", file=sys.stderr)
        return 1
    print("PASS: altered external AAD rejected")
    print("PASS: altered payload rejected")
    print("NOTE: fixture-only validation; no production protocol approval or parser conformance is implied")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
