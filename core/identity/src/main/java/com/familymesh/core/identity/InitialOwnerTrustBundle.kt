package com.familymesh.core.identity

import com.familymesh.core.common.domain.InitialOwnerTrustRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlinx.coroutines.flow.first

/**
 * Transferable, self-signed statement of the initial owner's public identity.
 *
 * A valid signature proves possession of the included key only. It does NOT
 * authenticate that key as the family's owner; recipients must compare its
 * fingerprint through an independent trusted channel before pinning it.
 */
class InitialOwnerTrustBundle(
    val familySpaceId: String,
    val ownerMemberId: String,
    val ownerDeviceKeyId: String,
    val establishedAtEpochMillis: Long,
    publicKeyBytes: ByteArray,
    signature: ByteArray,
) {
    private val publicKeyValue = publicKeyBytes.copyOf()
    private val signatureValue = signature.copyOf()
    val publicKeyBytes: ByteArray get() = publicKeyValue.copyOf()
    val signature: ByteArray get() = signatureValue.copyOf()

    init {
        require(familySpaceId.isNotBlank() && familySpaceId.length <= MAX_FIELD_BYTES)
        require(ownerMemberId.isNotBlank() && ownerMemberId.length <= MAX_FIELD_BYTES)
        require(ownerDeviceKeyId.isNotBlank() && ownerDeviceKeyId.length <= DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        require(establishedAtEpochMillis >= 0L)
        require(publicKeyValue.isNotEmpty() && publicKeyValue.size <= MAX_PUBLIC_KEY_BYTES)
        require(signatureValue.isNotEmpty() && signatureValue.size <= MAX_SIGNATURE_BYTES)
    }

    internal fun signingBytes(): ByteArray = payload(
        familySpaceId, ownerMemberId, ownerDeviceKeyId, establishedAtEpochMillis, publicKeyValue,
    )

    companion object {
        const val MAX_FIELD_BYTES = 256
        const val MAX_PUBLIC_KEY_BYTES = 1024
        const val MAX_SIGNATURE_BYTES = 256
    }
}

/** Creates a bundle only when the current device key matches the local owner anchor. */
class InitialOwnerTrustBundleService(
    private val trustRepository: InitialOwnerTrustRepository,
    private val keyManager: DeviceKeyManager,
    private val messageSigner: DeviceMessageSigner,
) {
    suspend fun create(familySpaceId: String): InitialOwnerTrustBundle {
        require(familySpaceId.isNotBlank())
        val trust = trustRepository.observeInitialOwners().first()
            .singleOrNull { it.familySpaceId == familySpaceId }
            ?: throw IllegalStateException("No unique local initial-owner trust record exists.")
        val identity = keyManager.getPublicIdentity()
            ?: throw IllegalStateException("This device has no signing identity.")
        require(identity.keyId == trust.ownerDeviceKeyId) {
            "The current device is not the locally trusted initial owner."
        }
        require(DeviceKeyFingerprint.fromEncodedPublicKey(identity.publicKeyBytes) == identity.keyId) {
            "The local public-key fingerprint is invalid."
        }

        val unsigned = InitialOwnerTrustBundle(
            familySpaceId = trust.familySpaceId,
            ownerMemberId = trust.ownerMemberId,
            ownerDeviceKeyId = identity.keyId,
            establishedAtEpochMillis = trust.establishedAtEpochMillis,
            publicKeyBytes = identity.publicKeyBytes,
            signature = byteArrayOf(1),
        )
        return InitialOwnerTrustBundle(
            familySpaceId = unsigned.familySpaceId,
            ownerMemberId = unsigned.ownerMemberId,
            ownerDeviceKeyId = unsigned.ownerDeviceKeyId,
            establishedAtEpochMillis = unsigned.establishedAtEpochMillis,
            publicKeyBytes = unsigned.publicKeyBytes,
            signature = messageSigner.sign(unsigned.signingBytes()),
        )
    }
}

/** Validates internal consistency and self-signature, not external owner trust. */
object InitialOwnerTrustBundleVerifier {
    fun verify(bundle: InitialOwnerTrustBundle): Boolean = try {
        if (DeviceKeyFingerprint.fromEncodedPublicKey(bundle.publicKeyBytes) != bundle.ownerDeviceKeyId) {
            return false
        }
        val publicKey = KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(bundle.publicKeyBytes),
        )
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(bundle.signingBytes())
            verify(bundle.signature)
        }
    } catch (_: Exception) {
        false
    }
}

/** Bounded v1 text envelope suitable for QR/text transfer. */
object InitialOwnerTrustBundleCodec {
    private const val PREFIX = "FMOT1."
    private const val VERSION = 1
    private const val MAX_PAYLOAD_BYTES = 2048
    private const val MAX_TEXT_LENGTH = 3000

    fun encode(bundle: InitialOwnerTrustBundle): String {
        require(InitialOwnerTrustBundleVerifier.verify(bundle)) { "Owner trust bundle is invalid." }
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(VERSION)
            out.writeUtf8(bundle.familySpaceId)
            out.writeUtf8(bundle.ownerMemberId)
            out.writeUtf8(bundle.ownerDeviceKeyId)
            out.writeLong(bundle.establishedAtEpochMillis)
            out.writeBounded(bundle.publicKeyBytes, InitialOwnerTrustBundle.MAX_PUBLIC_KEY_BYTES)
            out.writeBounded(bundle.signature, InitialOwnerTrustBundle.MAX_SIGNATURE_BYTES)
        }
        val payload = bytes.toByteArray()
        require(payload.size <= MAX_PAYLOAD_BYTES)
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    fun decode(text: String): InitialOwnerTrustBundle {
        require(text.length <= MAX_TEXT_LENGTH && text.startsWith(PREFIX)) {
            "Unsupported or oversized owner trust bundle."
        }
        val raw = Base64.getUrlDecoder().decode(text.removePrefix(PREFIX))
        require(raw.size <= MAX_PAYLOAD_BYTES)
        val input = DataInputStream(ByteArrayInputStream(raw))
        require(input.readInt() == VERSION) { "Unsupported owner trust bundle version." }
        val family = input.readUtf8()
        val member = input.readUtf8()
        val keyId = input.readUtf8(DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        val establishedAt = input.readLong()
        val publicKey = input.readBounded(InitialOwnerTrustBundle.MAX_PUBLIC_KEY_BYTES)
        val signature = input.readBounded(InitialOwnerTrustBundle.MAX_SIGNATURE_BYTES)
        require(input.available() == 0) { "Owner trust bundle contains trailing data." }
        return InitialOwnerTrustBundle(
            family, member, keyId, establishedAt, publicKey, signature,
        ).also {
            require(InitialOwnerTrustBundleVerifier.verify(it)) { "Owner trust bundle signature is invalid." }
        }
    }

    private fun DataOutputStream.writeUtf8(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= InitialOwnerTrustBundle.MAX_FIELD_BYTES)
        writeBounded(bytes, InitialOwnerTrustBundle.MAX_FIELD_BYTES)
    }

    private fun DataOutputStream.writeBounded(bytes: ByteArray, max: Int) {
        require(bytes.isNotEmpty() && bytes.size <= max)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readUtf8(max: Int = InitialOwnerTrustBundle.MAX_FIELD_BYTES): String {
        val bytes = readBounded(max)
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
            .also { require(it.isNotBlank()) }
    }

    private fun DataInputStream.readBounded(max: Int): ByteArray {
        val length = readInt()
        require(length in 1..max)
        return ByteArray(length).also { readFully(it) }
    }
}

private fun payload(
    familySpaceId: String,
    ownerMemberId: String,
    ownerDeviceKeyId: String,
    establishedAtEpochMillis: Long,
    publicKeyBytes: ByteArray,
): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
        out.write("FamilyMesh:initial-owner-trust:v1\u0000".toByteArray(Charsets.UTF_8))
        out.writeInt(1)
        listOf(familySpaceId, ownerMemberId, ownerDeviceKeyId).forEach { value ->
            val encoded = value.toByteArray(Charsets.UTF_8)
            out.writeInt(encoded.size)
            out.write(encoded)
        }
        out.writeLong(establishedAtEpochMillis)
        out.writeInt(publicKeyBytes.size)
        out.write(publicKeyBytes)
    }
    return bytes.toByteArray()
}
