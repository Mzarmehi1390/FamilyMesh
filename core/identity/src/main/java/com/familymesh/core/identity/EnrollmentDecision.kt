package com.familymesh.core.identity

import com.familymesh.core.common.domain.InitialOwnerTrustRepository
import com.familymesh.core.common.domain.PendingEnrollmentRequest
import com.familymesh.core.common.domain.TrustedInitialOwner
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

enum class EnrollmentDecisionOutcome { APPROVED, REJECTED }

/**
 * A signed decision about one exact enrollment request. Approval here is a
 * portable owner statement only; it does not create a member or grant access.
 */
class SignedEnrollmentDecision(
    val familySpaceId: String,
    val requestId: String,
    val applicantDeviceKeyId: String,
    val ownerDeviceKeyId: String,
    val outcome: EnrollmentDecisionOutcome,
    val decidedAtEpochMillis: Long,
    signature: ByteArray,
) {
    private val signatureValue = signature.copyOf()
    val signature: ByteArray get() = signatureValue.copyOf()

    init {
        require(familySpaceId.isNotBlank() && familySpaceId.length <= MAX_FIELD_BYTES)
        require(requestId.isNotBlank() && requestId.length <= MAX_FIELD_BYTES)
        require(applicantDeviceKeyId.isNotBlank() && applicantDeviceKeyId.length <= DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        require(ownerDeviceKeyId.isNotBlank() && ownerDeviceKeyId.length <= DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        require(decidedAtEpochMillis >= 0L)
        require(signatureValue.isNotEmpty() && signatureValue.size <= MAX_SIGNATURE_BYTES)
    }

    internal fun signingBytes(): ByteArray = decisionPayload(
        familySpaceId, requestId, applicantDeviceKeyId, ownerDeviceKeyId, outcome, decidedAtEpochMillis,
    )

    companion object {
        const val MAX_FIELD_BYTES = 256
        const val MAX_SIGNATURE_BYTES = 256
    }
}

/** Creates decisions only from the device matching the local initial-owner anchor. */
class EnrollmentDecisionSigner(
    private val trustRepository: InitialOwnerTrustRepository,
    private val keyManager: DeviceKeyManager,
    private val messageSigner: DeviceMessageSigner,
) {
    suspend fun sign(
        request: PendingEnrollmentRequest,
        outcome: EnrollmentDecisionOutcome,
        decidedAtEpochMillis: Long,
    ): SignedEnrollmentDecision {
        require(decidedAtEpochMillis >= request.createdAtEpochMillis &&
            decidedAtEpochMillis < request.expiresAtEpochMillis) { "Request is not within its decision window." }
        val trust = trustRepository.observeInitialOwners().first()
            .singleOrNull { it.familySpaceId == request.familySpaceId }
            ?: throw IllegalStateException("No unique local initial-owner anchor exists.")
        val identity = keyManager.getPublicIdentity()
            ?: throw IllegalStateException("This device has no signing identity.")
        require(identity.keyId == trust.ownerDeviceKeyId) {
            "The current device is not the locally trusted initial owner."
        }
        require(DeviceKeyFingerprint.fromEncodedPublicKey(identity.publicKeyBytes) == identity.keyId) {
            "The local public-key fingerprint is invalid."
        }
        val unsigned = SignedEnrollmentDecision(
            request.familySpaceId, request.requestId, request.deviceKeyId,
            identity.keyId, outcome, decidedAtEpochMillis, byteArrayOf(1),
        )
        return SignedEnrollmentDecision(
            unsigned.familySpaceId, unsigned.requestId, unsigned.applicantDeviceKeyId,
            unsigned.ownerDeviceKeyId, unsigned.outcome, unsigned.decidedAtEpochMillis,
            messageSigner.sign(unsigned.signingBytes()),
        )
    }
}

/** Verifies an owner decision against both the exact pending request and a pinned owner key. */
object EnrollmentDecisionVerifier {
    fun verify(
        decision: SignedEnrollmentDecision,
        request: PendingEnrollmentRequest,
        trustedOwner: TrustedInitialOwner,
        nowEpochMillis: Long,
    ): Boolean {
        return try {
        if (decision.familySpaceId != request.familySpaceId ||
            decision.requestId != request.requestId ||
            decision.applicantDeviceKeyId != request.deviceKeyId ||
            decision.familySpaceId != trustedOwner.familySpaceId ||
            decision.ownerDeviceKeyId != trustedOwner.ownerDeviceKeyId ||
            decision.decidedAtEpochMillis < request.createdAtEpochMillis ||
            decision.decidedAtEpochMillis >= request.expiresAtEpochMillis ||
            decision.decidedAtEpochMillis > nowEpochMillis
        ) return false

        val publicKeyBytes = Base64.getDecoder().decode(trustedOwner.ownerPublicKeyBase64)
        if (DeviceKeyFingerprint.fromEncodedPublicKey(publicKeyBytes) != trustedOwner.ownerDeviceKeyId) {
            return false
        }
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyBytes))
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(decision.signingBytes())
            verify(decision.signature)
        }
        } catch (_: Exception) {
            false
        }
    }
}

/** Bounded, versioned text envelope for manually transferring a signed decision. */
object EnrollmentDecisionTextCodec {
    private const val PREFIX = "FMOD1."
    private const val VERSION = 1
    private const val MAX_PAYLOAD_BYTES = 2048
    private const val MAX_TEXT_LENGTH = 3000

    fun encode(decision: SignedEnrollmentDecision): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(VERSION)
            out.writeUtf8(decision.familySpaceId, SignedEnrollmentDecision.MAX_FIELD_BYTES)
            out.writeUtf8(decision.requestId, SignedEnrollmentDecision.MAX_FIELD_BYTES)
            out.writeUtf8(decision.applicantDeviceKeyId, DevicePublicIdentity.MAX_KEY_ID_LENGTH)
            out.writeUtf8(decision.ownerDeviceKeyId, DevicePublicIdentity.MAX_KEY_ID_LENGTH)
            out.writeUtf8(decision.outcome.name, 16)
            out.writeLong(decision.decidedAtEpochMillis)
            out.writeBounded(decision.signature, SignedEnrollmentDecision.MAX_SIGNATURE_BYTES)
        }
        val payload = bytes.toByteArray()
        require(payload.size <= MAX_PAYLOAD_BYTES)
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    fun decode(text: String): SignedEnrollmentDecision {
        require(text.length <= MAX_TEXT_LENGTH && text.startsWith(PREFIX)) { "Unsupported or oversized decision." }
        val raw = Base64.getUrlDecoder().decode(text.removePrefix(PREFIX))
        require(raw.isNotEmpty() && raw.size <= MAX_PAYLOAD_BYTES)
        val input = DataInputStream(ByteArrayInputStream(raw))
        require(input.readInt() == VERSION) { "Unsupported decision version." }
        val family = input.readUtf8(SignedEnrollmentDecision.MAX_FIELD_BYTES)
        val request = input.readUtf8(SignedEnrollmentDecision.MAX_FIELD_BYTES)
        val applicant = input.readUtf8(DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        val owner = input.readUtf8(DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        val outcome = try {
            EnrollmentDecisionOutcome.valueOf(input.readUtf8(16))
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown enrollment decision outcome.")
        }
        val timestamp = input.readLong()
        val signature = input.readBounded(SignedEnrollmentDecision.MAX_SIGNATURE_BYTES)
        require(input.available() == 0) { "Decision contains trailing data." }
        return SignedEnrollmentDecision(family, request, applicant, owner, outcome, timestamp, signature)
    }

    private fun DataOutputStream.writeUtf8(value: String, max: Int) =
        writeBounded(value.toByteArray(Charsets.UTF_8), max).also {
            require(value.isNotBlank()) { "Text field must not be blank." }
        }

    private fun DataOutputStream.writeBounded(value: ByteArray, max: Int) {
        require(value.isNotEmpty() && value.size <= max) { "Invalid field length." }
        writeInt(value.size)
        write(value)
    }

    private fun DataInputStream.readUtf8(max: Int): String =
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(readBounded(max))).toString()
            .also { require(it.isNotBlank()) { "Text field must not be blank." } }

    private fun DataInputStream.readBounded(max: Int): ByteArray {
        val length = readInt()
        require(length in 1..max) { "Invalid field length." }
        return ByteArray(length).also { readFully(it) }
    }
}

private fun decisionPayload(
    familySpaceId: String,
    requestId: String,
    applicantDeviceKeyId: String,
    ownerDeviceKeyId: String,
    outcome: EnrollmentDecisionOutcome,
    decidedAtEpochMillis: Long,
): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
        out.write("FamilyMesh:enrollment-decision:v1\u0000".toByteArray(Charsets.UTF_8))
        out.writeInt(1)
        listOf(familySpaceId, requestId, applicantDeviceKeyId, ownerDeviceKeyId, outcome.name).forEach { value ->
            val encoded = value.toByteArray(Charsets.UTF_8)
            require(encoded.isNotEmpty() && encoded.size <= SignedEnrollmentDecision.MAX_FIELD_BYTES)
            out.writeInt(encoded.size)
            out.write(encoded)
        }
        out.writeLong(decidedAtEpochMillis)
    }
    return bytes.toByteArray()
}
