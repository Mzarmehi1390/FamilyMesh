package com.familymesh.core.identity

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

/** Applicant-signed acceptance of one exact, owner-signed approval. This does not itself create membership. */
class SignedEnrollmentAcceptance(
    val decision: SignedEnrollmentDecision,
    val applicantDeviceKeyId: String,
    applicantPublicKeyBytes: ByteArray,
    val acceptedAtEpochMillis: Long,
    signature: ByteArray,
) {
    private val publicKeyValue = applicantPublicKeyBytes.copyOf()
    private val signatureValue = signature.copyOf()
    val applicantPublicKeyBytes: ByteArray get() = publicKeyValue.copyOf()
    val signature: ByteArray get() = signatureValue.copyOf()

    init {
        require(applicantDeviceKeyId.isNotBlank() && applicantDeviceKeyId.length <= DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        require(publicKeyValue.isNotEmpty() && publicKeyValue.size <= MAX_PUBLIC_KEY_BYTES)
        require(acceptedAtEpochMillis >= 0L)
        require(signatureValue.isNotEmpty() && signatureValue.size <= MAX_SIGNATURE_BYTES)
    }

    internal fun signingBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.write(DOMAIN)
            data.writeInt(VERSION)
            data.writeUtf8(EnrollmentDecisionTextCodec.encode(decision), MAX_DECISION_TEXT_LENGTH)
            data.writeUtf8(applicantDeviceKeyId, DevicePublicIdentity.MAX_KEY_ID_LENGTH)
            data.writeBounded(publicKeyValue, MAX_PUBLIC_KEY_BYTES)
            data.writeLong(acceptedAtEpochMillis)
        }
        return output.toByteArray()
    }

    companion object {
        const val MAX_PUBLIC_KEY_BYTES = 512
        const val MAX_SIGNATURE_BYTES = 256
        const val MAX_DECISION_TEXT_LENGTH = 3000
        internal const val VERSION = 1
        internal val DOMAIN = "FamilyMesh:enrollment-acceptance:v1\u0000".toByteArray(Charsets.UTF_8)
    }
}

/** Signs an approval receipt using the applicant's existing device key. */
class EnrollmentAcceptanceSigner(private val messageSigner: DeviceMessageSigner) {
    suspend fun sign(
        decision: SignedEnrollmentDecision,
        applicantIdentity: DevicePublicIdentity,
        acceptedAtEpochMillis: Long,
    ): SignedEnrollmentAcceptance {
        require(decision.outcome == EnrollmentDecisionOutcome.APPROVED) {
            "Only an approved decision can be accepted."
        }
        require(decision.applicantDeviceKeyId == applicantIdentity.keyId) {
            "The decision is not addressed to this device."
        }
        require(DeviceKeyFingerprint.fromEncodedPublicKey(applicantIdentity.publicKeyBytes) == applicantIdentity.keyId) {
            "Applicant public-key fingerprint is invalid."
        }
        require(acceptedAtEpochMillis >= decision.decidedAtEpochMillis) {
            "Acceptance cannot predate the owner decision."
        }
        val unsigned = SignedEnrollmentAcceptance(
            decision, applicantIdentity.keyId, applicantIdentity.publicKeyBytes,
            acceptedAtEpochMillis, byteArrayOf(1),
        )
        return SignedEnrollmentAcceptance(
            decision, applicantIdentity.keyId, applicantIdentity.publicKeyBytes,
            acceptedAtEpochMillis, messageSigner.sign(unsigned.signingBytes()),
        )
    }
}

/** Verifies both signatures and binds the acceptance to the exact pending request. */
object EnrollmentAcceptanceVerifier {
    fun verify(
        acceptance: SignedEnrollmentAcceptance,
        request: PendingEnrollmentRequest,
        trustedOwner: TrustedInitialOwner,
        nowEpochMillis: Long,
    ): Boolean {
        return try {
            val decision = acceptance.decision
            if (nowEpochMillis < 0L ||
                decision.outcome != EnrollmentDecisionOutcome.APPROVED ||
                acceptance.applicantDeviceKeyId != request.deviceKeyId ||
                decision.applicantDeviceKeyId != request.deviceKeyId ||
                acceptance.acceptedAtEpochMillis < decision.decidedAtEpochMillis ||
                acceptance.acceptedAtEpochMillis >= request.expiresAtEpochMillis ||
                acceptance.acceptedAtEpochMillis > nowEpochMillis ||
                !EnrollmentDecisionVerifier.verify(decision, request, trustedOwner, nowEpochMillis)
            ) {
                false
            } else {
                val publicKeyBytes = acceptance.applicantPublicKeyBytes
                if (DeviceKeyFingerprint.fromEncodedPublicKey(publicKeyBytes) != request.deviceKeyId) {
                    false
                } else {
                    val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyBytes))
                    Signature.getInstance("SHA256withECDSA").run {
                        initVerify(publicKey)
                        update(acceptance.signingBytes())
                        verify(acceptance.signature)
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}

/** Bounded, versioned text envelope for manual transfer of the applicant's signed acceptance. */
object EnrollmentAcceptanceTextCodec {
    private const val PREFIX = "FMJA1."
    private const val MAX_TEXT_LENGTH = 6000
    private const val MAX_PAYLOAD_BYTES = 4096

    fun encode(acceptance: SignedEnrollmentAcceptance): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(SignedEnrollmentAcceptance.VERSION)
            output.writeUtf8(EnrollmentDecisionTextCodec.encode(acceptance.decision), SignedEnrollmentAcceptance.MAX_DECISION_TEXT_LENGTH)
            output.writeUtf8(acceptance.applicantDeviceKeyId, DevicePublicIdentity.MAX_KEY_ID_LENGTH)
            output.writeBounded(acceptance.applicantPublicKeyBytes, SignedEnrollmentAcceptance.MAX_PUBLIC_KEY_BYTES)
            output.writeLong(acceptance.acceptedAtEpochMillis)
            output.writeBounded(acceptance.signature, SignedEnrollmentAcceptance.MAX_SIGNATURE_BYTES)
        }
        val payload = bytes.toByteArray()
        require(payload.size <= MAX_PAYLOAD_BYTES)
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    fun decode(text: String): SignedEnrollmentAcceptance {
        require(text.length <= MAX_TEXT_LENGTH && text.startsWith(PREFIX)) { "Unsupported or oversized acceptance." }
        val raw = CanonicalBase64Url.decode(text.removePrefix(PREFIX))
        require(raw.isNotEmpty() && raw.size <= MAX_PAYLOAD_BYTES)
        val input = DataInputStream(ByteArrayInputStream(raw))
        require(input.readInt() == SignedEnrollmentAcceptance.VERSION) { "Unsupported acceptance version." }
        val decision = EnrollmentDecisionTextCodec.decode(input.readUtf8(SignedEnrollmentAcceptance.MAX_DECISION_TEXT_LENGTH))
        val applicantKeyId = input.readUtf8(DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        val publicKey = input.readBounded(SignedEnrollmentAcceptance.MAX_PUBLIC_KEY_BYTES)
        val acceptedAt = input.readLong()
        val signature = input.readBounded(SignedEnrollmentAcceptance.MAX_SIGNATURE_BYTES)
        require(input.available() == 0) { "Acceptance contains trailing data." }
        return SignedEnrollmentAcceptance(decision, applicantKeyId, publicKey, acceptedAt, signature)
    }

    private fun DataOutputStream.writeUtf8(value: String, max: Int) {
        require(value.isNotBlank())
        writeBounded(value.toByteArray(Charsets.UTF_8), max)
    }

    private fun DataOutputStream.writeBounded(value: ByteArray, max: Int) {
        require(value.isNotEmpty() && value.size <= max)
        writeInt(value.size)
        write(value)
    }

    private fun DataInputStream.readUtf8(max: Int): String =
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(readBounded(max))).toString()
            .also { require(it.isNotBlank()) }

    private fun DataInputStream.readBounded(max: Int): ByteArray {
        val length = readInt()
        require(length in 1..max)
        return ByteArray(length).also { readFully(it) }
    }
}

private fun DataOutputStream.writeUtf8(value: String, max: Int) {
    require(value.isNotBlank())
    writeBounded(value.toByteArray(Charsets.UTF_8), max)
}

private fun DataOutputStream.writeBounded(value: ByteArray, max: Int) {
    require(value.isNotEmpty() && value.size <= max)
    writeInt(value.size)
    write(value)
}
