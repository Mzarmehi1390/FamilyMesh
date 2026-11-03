package com.familymesh.core.identity

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialOwnerTrustBundleTest {
    private val pair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec("secp256r1"))
        generateKeyPair()
    }
    private val identity = DevicePublicIdentity(
        DeviceKeyFingerprint.fromEncodedPublicKey(pair.public.encoded),
        pair.public.encoded,
        100L,
    )
    private val signer = DeviceMessageSigner { message ->
        Signature.getInstance("SHA256withECDSA").run {
            initSign(pair.private)
            update(message)
            sign()
        }
    }

    @Test
    fun signedBundleRoundTripsThroughTextCodec() = runBlocking {
        val bundle = unsignedBundle().let { unsigned ->
            InitialOwnerTrustBundle(
                unsigned.familySpaceId, unsigned.ownerMemberId, unsigned.ownerDeviceKeyId,
                unsigned.establishedAtEpochMillis, unsigned.publicKeyBytes,
                signer.sign(unsigned.signingBytes()),
            )
        }
        val encoded = InitialOwnerTrustBundleCodec.encode(bundle)
        val decoded = InitialOwnerTrustBundleCodec.decode(encoded)
        assertTrue(InitialOwnerTrustBundleVerifier.verify(decoded))
        assertEquals(bundle.familySpaceId, decoded.familySpaceId)
        assertEquals(bundle.ownerMemberId, decoded.ownerMemberId)
        assertEquals(bundle.ownerDeviceKeyId, decoded.ownerDeviceKeyId)
        assertEquals(bundle.publicKeyBytes.toList(), decoded.publicKeyBytes.toList())
    }

    @Test
    fun alteredSignatureIsRejected() = runBlocking {
        val unsigned = unsignedBundle()
        val signed = InitialOwnerTrustBundle(
            unsigned.familySpaceId, unsigned.ownerMemberId, unsigned.ownerDeviceKeyId,
            unsigned.establishedAtEpochMillis, unsigned.publicKeyBytes,
            signer.sign(unsigned.signingBytes()),
        )
        val tampered = InitialOwnerTrustBundle(
            signed.familySpaceId, signed.ownerMemberId, signed.ownerDeviceKeyId,
            signed.establishedAtEpochMillis, signed.publicKeyBytes,
            signed.signature.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() },
        )
        assertFalse(InitialOwnerTrustBundleVerifier.verify(tampered))
    }

    @Test
    fun keyIdMustMatchIncludedPublicKey() = runBlocking {
        val unsigned = unsignedBundle()
        val wrong = InitialOwnerTrustBundle(
            unsigned.familySpaceId, unsigned.ownerMemberId, "wrong-key-id",
            unsigned.establishedAtEpochMillis, unsigned.publicKeyBytes, byteArrayOf(1),
        )
        assertFalse(InitialOwnerTrustBundleVerifier.verify(wrong))
    }

    @Test
    fun codecRejectsTrailingBytes() = runBlocking {
        val unsigned = unsignedBundle()
        val signed = InitialOwnerTrustBundle(
            unsigned.familySpaceId, unsigned.ownerMemberId, unsigned.ownerDeviceKeyId,
            unsigned.establishedAtEpochMillis, unsigned.publicKeyBytes,
            signer.sign(unsigned.signingBytes()),
        )
        val encoded = InitialOwnerTrustBundleCodec.encode(signed)
        val raw = java.util.Base64.getUrlDecoder().decode(encoded.removePrefix("FMOT1."))
        val altered = "FMOT1." + java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw + byteArrayOf(0))
        try {
            InitialOwnerTrustBundleCodec.decode(altered)
            throw AssertionError("Expected trailing bytes to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private fun unsignedBundle() = InitialOwnerTrustBundle(
        familySpaceId = "family-1",
        ownerMemberId = "owner-member-1",
        ownerDeviceKeyId = identity.keyId,
        establishedAtEpochMillis = 200L,
        publicKeyBytes = identity.publicKeyBytes,
        signature = byteArrayOf(1),
    )
}
