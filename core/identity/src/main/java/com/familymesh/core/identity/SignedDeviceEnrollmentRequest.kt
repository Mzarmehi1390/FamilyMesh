package com.familymesh.core.identity

import com.familymesh.core.common.domain.DeviceEnrollmentRequest
import com.familymesh.core.common.domain.EnrollmentRequestStatus
import com.familymesh.core.common.domain.FamilyRole
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.SecureRandom

/**
 * A device-key-signed snapshot of a pending enrollment request.
 *
 * This proves possession of the signing key for these exact request fields.
 * It does not prove that the requested role was authorized by a family owner,
 * that the request is fresh globally, or that membership was approved.
 */
class SignedDeviceEnrollmentRequest(
    val requestId: String,
    val familySpaceId: String,
    val deviceKeyId: String,
    val requestedRole: FamilyRole,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    nonce: ByteArray,
    signature: ByteArray,
) {
    private val nonceValue = nonce.copyOf()
    private val signatureValue = signature.copyOf()

    val nonce: ByteArray
        get() = nonceValue.copyOf()

    val signature: ByteArray
        get() = signatureValue.copyOf()

    init {
        require(requestId.isNotBlank() && requestId.length <= MAX_IDENTIFIER_LENGTH)
        require(familySpaceId.isNotBlank() && familySpaceId.length <= MAX_IDENTIFIER_LENGTH)
        require(deviceKeyId.isNotBlank() && deviceKeyId.length <= DevicePublicIdentity.MAX_KEY_ID_LENGTH)
        require(createdAtEpochMillis >= 0L)
        require(expiresAtEpochMillis > createdAtEpochMillis)
        require(nonceValue.size == NONCE_LENGTH)
        require(signatureValue.isNotEmpty() && signatureValue.size <= MAX_SIGNATURE_LENGTH)
    }

    /** True only while the signed request is inside its declared validity window. */
    fun isWithinValidityWindow(nowEpochMillis: Long): Boolean =
        nowEpochMillis >= createdAtEpochMillis && nowEpochMillis < expiresAtEpochMillis

    internal fun signingBytes(): ByteArray = EnrollmentRequestSigningPayload.encode(
        requestId = requestId,
        familySpaceId = familySpaceId,
        deviceKeyId = deviceKeyId,
        requestedRole = requestedRole,
        createdAtEpochMillis = createdAtEpochMillis,
        expiresAtEpochMillis = expiresAtEpochMillis,
        nonce = nonceValue,
    )

    companion object {
        const val NONCE_LENGTH = 32
        const val MAX_IDENTIFIER_LENGTH = 256
        const val MAX_SIGNATURE_LENGTH = 256
    }
}

/**
 * Creates a signed proof using a device key that matches the request's key ID.
 * A new cryptographically random nonce is generated for every proof.
 */
class DeviceEnrollmentProofSigner(
    private val messageSigner: DeviceMessageSigner,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    suspend fun sign(
        request: DeviceEnrollmentRequest,
        identity: DevicePublicIdentity,
    ): SignedDeviceEnrollmentRequest {
        require(request.status == EnrollmentRequestStatus.PENDING) {
            "Only pending enrollment requests can be signed."
        }
        require(request.deviceKeyId == identity.keyId) {
            "Enrollment request device key does not match the signing identity."
        }

        val nonce = ByteArray(SignedDeviceEnrollmentRequest.NONCE_LENGTH)
        secureRandom.nextBytes(nonce)
        val unsigned = SignedDeviceEnrollmentRequest(
            requestId = request.id,
            familySpaceId = request.familySpaceId,
            deviceKeyId = request.deviceKeyId,
            requestedRole = request.requestedRole,
            createdAtEpochMillis = request.createdAtEpochMillis,
            expiresAtEpochMillis = request.expiresAtEpochMillis,
            nonce = nonce,
            signature = byteArrayOf(1), // Replaced immediately after signing.
        )
        val signature = messageSigner.sign(unsigned.signingBytes())
        return SignedDeviceEnrollmentRequest(
            requestId = request.id,
            familySpaceId = request.familySpaceId,
            deviceKeyId = request.deviceKeyId,
            requestedRole = request.requestedRole,
            createdAtEpochMillis = request.createdAtEpochMillis,
            expiresAtEpochMillis = request.expiresAtEpochMillis,
            nonce = nonce,
            signature = signature,
        )
    }
}

/**
 * Verifies the request signature against an independently supplied public key.
 * It never trusts the role as an authorization grant.
 */
object DeviceEnrollmentProofVerifier {
    fun verifySignature(
        proof: SignedDeviceEnrollmentRequest,
        identity: DevicePublicIdentity,
    ): Boolean {
        return try {
            if (proof.deviceKeyId != identity.keyId) return false
            if (DeviceKeyFingerprint.fromEncodedPublicKey(identity.publicKeyBytes) != identity.keyId) {
                return false
            }
            val publicKey = java.security.KeyFactory.getInstance("EC").generatePublic(
                java.security.spec.X509EncodedKeySpec(identity.publicKeyBytes),
            )
            if (!publicKey.algorithm.equals("EC", ignoreCase = true)) return false
            java.security.Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initVerify(publicKey)
                update(proof.signingBytes())
                verify(proof.signature)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun verify(
        proof: SignedDeviceEnrollmentRequest,
        identity: DevicePublicIdentity,
        nowEpochMillis: Long,
    ): Boolean = proof.isWithinValidityWindow(nowEpochMillis) &&
        verifySignature(proof, identity)

    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
}

/**
 * Fixed-order, length-prefixed binary payload. This is a local v1 signing
 * envelope, not CBOR/COSE serialization and not a general-purpose codec.
 */
private object EnrollmentRequestSigningPayload {
    private val domain = "FamilyMesh:device-enrollment-request:v1\u0000"
        .toByteArray(Charsets.UTF_8)

    fun encode(
        requestId: String,
        familySpaceId: String,
        deviceKeyId: String,
        requestedRole: FamilyRole,
        createdAtEpochMillis: Long,
        expiresAtEpochMillis: Long,
        nonce: ByteArray,
    ): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.write(domain)
            output.writeInt(PROTOCOL_VERSION)
            output.writeUtf8(requestId)
            output.writeUtf8(familySpaceId)
            output.writeUtf8(deviceKeyId)
            output.writeUtf8(requestedRole.name)
            output.writeLong(createdAtEpochMillis)
            output.writeLong(expiresAtEpochMillis)
            output.writeInt(nonce.size)
            output.write(nonce)
        }
        return bytes.toByteArray()
    }

    private fun DataOutputStream.writeUtf8(value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        require(encoded.isNotEmpty() && encoded.size <= SignedDeviceEnrollmentRequest.MAX_IDENTIFIER_LENGTH)
        writeInt(encoded.size)
        write(encoded)
    }

    private const val PROTOCOL_VERSION = 1
}
