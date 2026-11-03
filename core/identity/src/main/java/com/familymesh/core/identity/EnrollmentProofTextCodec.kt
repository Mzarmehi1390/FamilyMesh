package com.familymesh.core.identity

import com.familymesh.core.common.domain.FamilyRole
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Base64

/**
 * Bounded, versioned text transport for a signed enrollment proof and its
 * claimed public identity. The public key is untrusted until its fingerprint
 * and the proof signature are verified.
 *
 * This is a transfer envelope, not an authenticated channel or COSE format.
 */
object EnrollmentProofTextCodec {
    private const val PREFIX = "FMER1."
    private const val VERSION = 1
    private const val MAX_ENVELOPE_BYTES = 4096
    private const val MAX_PUBLIC_KEY_BYTES = 1024
    private const val MAX_TEXT_LENGTH = 6000

    fun encode(
        proof: SignedDeviceEnrollmentRequest,
        identity: DevicePublicIdentity,
    ): String {
        require(proof.deviceKeyId == identity.keyId) { "Proof and public identity key IDs differ." }
        require(DeviceKeyFingerprint.fromEncodedPublicKey(identity.publicKeyBytes) == identity.keyId) {
            "Public identity key fingerprint is invalid."
        }

        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(VERSION)
            output.writeLong(identity.createdAtEpochMillis)
            output.writeBoundedBytes(identity.publicKeyBytes, MAX_PUBLIC_KEY_BYTES)
            output.writeUtf8(proof.requestId, SignedDeviceEnrollmentRequest.MAX_IDENTIFIER_LENGTH)
            output.writeUtf8(proof.familySpaceId, SignedDeviceEnrollmentRequest.MAX_IDENTIFIER_LENGTH)
            output.writeUtf8(proof.deviceKeyId, DevicePublicIdentity.MAX_KEY_ID_LENGTH)
            output.writeUtf8(proof.requestedRole.name, 16)
            output.writeLong(proof.createdAtEpochMillis)
            output.writeLong(proof.expiresAtEpochMillis)
            output.writeBoundedBytes(proof.nonce, SignedDeviceEnrollmentRequest.NONCE_LENGTH)
            output.writeBoundedBytes(proof.signature, SignedDeviceEnrollmentRequest.MAX_SIGNATURE_LENGTH)
        }
        val payload = bytes.toByteArray()
        require(payload.size <= MAX_ENVELOPE_BYTES) { "Enrollment proof is too large." }
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    fun decode(encoded: String): DecodedEnrollmentProof {
        require(encoded.length <= MAX_TEXT_LENGTH) { "Enrollment proof text is too long." }
        require(encoded.startsWith(PREFIX)) { "Unsupported enrollment proof format." }
        val payloadText = encoded.substring(PREFIX.length)
        require(payloadText.isNotEmpty()) { "Enrollment proof is empty." }
        val payload = try {
            CanonicalBase64Url.decode(payloadText)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Enrollment proof is not valid Base64URL.")
        }
        require(payload.size <= MAX_ENVELOPE_BYTES) { "Enrollment proof payload is too large." }

        val input = DataInputStream(ByteArrayInputStream(payload))
        val version = input.readInt()
        require(version == VERSION) { "Unsupported enrollment proof version." }
        val identityCreatedAt = input.readLong()
        require(identityCreatedAt >= 0L) { "Invalid public identity timestamp." }
        val publicKey = input.readBoundedBytes(MAX_PUBLIC_KEY_BYTES)
        val requestId = input.readUtf8(SignedDeviceEnrollmentRequest.MAX_IDENTIFIER_LENGTH)
        val familySpaceId = input.readUtf8(SignedDeviceEnrollmentRequest.MAX_IDENTIFIER_LENGTH)
        val keyId = input.readUtf8(DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        val roleName = input.readUtf8(16)
        val role = try {
            FamilyRole.valueOf(roleName)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown requested family role.")
        }
        val createdAt = input.readLong()
        val expiresAt = input.readLong()
        val nonce = input.readBoundedBytes(SignedDeviceEnrollmentRequest.NONCE_LENGTH)
        val signature = input.readBoundedBytes(SignedDeviceEnrollmentRequest.MAX_SIGNATURE_LENGTH)
        require(input.available() == 0) { "Enrollment proof contains trailing data." }

        val identity = DevicePublicIdentity(
            keyId = keyId,
            publicKeyBytes = publicKey,
            createdAtEpochMillis = identityCreatedAt,
        )
        val proof = SignedDeviceEnrollmentRequest(
            requestId = requestId,
            familySpaceId = familySpaceId,
            deviceKeyId = keyId,
            requestedRole = role,
            createdAtEpochMillis = createdAt,
            expiresAtEpochMillis = expiresAt,
            nonce = nonce,
            signature = signature,
        )
        return DecodedEnrollmentProof(proof, identity)
    }

    private fun DataOutputStream.writeUtf8(value: String, maxBytes: Int) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= maxBytes) { "Text field is empty or too large." }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.writeBoundedBytes(value: ByteArray, maxBytes: Int) {
        require(value.isNotEmpty() && value.size <= maxBytes) { "Binary field is empty or too large." }
        writeInt(value.size)
        write(value)
    }

    private fun DataInputStream.readUtf8(maxBytes: Int): String {
        val bytes = readBoundedBytes(maxBytes)
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
            .also { require(it.isNotBlank()) { "Text field must not be blank." } }
    }

    private fun DataInputStream.readBoundedBytes(maxBytes: Int): ByteArray {
        val length = readInt()
        require(length in 1..maxBytes) { "Invalid field length." }
        val bytes = ByteArray(length)
        readFully(bytes)
        return bytes
    }
}

data class DecodedEnrollmentProof(
    val proof: SignedDeviceEnrollmentRequest,
    val identity: DevicePublicIdentity,
)
