package com.familymesh.core.storage.family

import com.familymesh.core.common.domain.AuthenticatedFamilyDevice
import com.familymesh.core.common.domain.EnrollmentFinalizationRepository
import com.familymesh.core.common.domain.EnrollmentFinalizationResult
import com.familymesh.core.common.domain.FamilyRole
import com.familymesh.core.common.domain.PendingEnrollmentRequest

class RoomEnrollmentFinalizationRepository(
    private val dao: EnrollmentNonceDao,
) : EnrollmentFinalizationRepository {
    override fun observeAuthenticatedDevices(familySpaceId: String) =
        dao.observeAuthenticatedDevices(familySpaceId).let { flow ->
            kotlinx.coroutines.flow.map(flow) { entities ->
                entities.map { entity ->
                    AuthenticatedFamilyDevice(
                        familySpaceId = entity.familySpaceId,
                        requestId = entity.requestId,
                        deviceKeyId = entity.deviceKeyId,
                        publicKeyBase64 = entity.publicKeyBase64,
                        role = FamilyRole.valueOf(entity.role),
                        joinedAtEpochMillis = entity.joinedAtEpochMillis,
                        acceptanceSignatureBase64 = entity.acceptanceSignatureBase64,
                    )
                }
            }
        }

    override suspend fun findPendingRequest(familySpaceId: String, requestId: String): PendingEnrollmentRequest? =
        dao.getPendingRequest(familySpaceId, requestId)?.let {
            PendingEnrollmentRequest(
                requestId = it.requestId,
                familySpaceId = it.familySpaceId,
                deviceKeyId = it.deviceKeyId,
                requestedRole = FamilyRole.valueOf(it.requestedRole),
                createdAtEpochMillis = it.createdAtEpochMillis,
                expiresAtEpochMillis = it.expiresAtEpochMillis,
                receivedAtEpochMillis = it.receivedAtEpochMillis,
            )
        }

    override suspend fun findFinalized(
        familySpaceId: String,
        requestId: String,
        deviceKeyId: String,
        acceptanceSignatureBase64: String,
    ): EnrollmentFinalizationResult {
        val existing = dao.getAuthenticatedDevice(familySpaceId, requestId)
            ?: return EnrollmentFinalizationResult.REQUEST_NOT_PENDING
        return if (existing.deviceKeyId == deviceKeyId &&
            existing.acceptanceSignatureBase64 == acceptanceSignatureBase64
        ) EnrollmentFinalizationResult.ALREADY_FINALIZED else EnrollmentFinalizationResult.CONFLICT
    }

    override suspend fun finalizeEnrollment(device: AuthenticatedFamilyDevice): EnrollmentFinalizationResult {
        require(device.publicKeyBase64.length <= MAX_PUBLIC_KEY_BASE64_LENGTH)
        require(device.publicKeyBase64.matches(Regex("[A-Za-z0-9+/]+={0,2}")))
        require(device.acceptanceSignatureBase64.length <= MAX_SIGNATURE_BASE64_LENGTH)
        require(device.acceptanceSignatureBase64.matches(Regex("[A-Za-z0-9+/]+={0,2}")))
        return when (dao.finalizeAuthenticatedDevice(
            AuthenticatedFamilyDeviceEntity(
                familySpaceId = device.familySpaceId,
                requestId = device.requestId,
                deviceKeyId = device.deviceKeyId,
                publicKeyBase64 = device.publicKeyBase64,
                role = device.role.name,
                joinedAtEpochMillis = device.joinedAtEpochMillis,
                acceptanceSignatureBase64 = device.acceptanceSignatureBase64,
            )
        )) {
            "FINALIZED" -> EnrollmentFinalizationResult.FINALIZED
            "ALREADY_FINALIZED" -> EnrollmentFinalizationResult.ALREADY_FINALIZED
            "REQUEST_NOT_PENDING" -> EnrollmentFinalizationResult.REQUEST_NOT_PENDING
            else -> EnrollmentFinalizationResult.CONFLICT
        }
    }

    private companion object {
        const val MAX_PUBLIC_KEY_BASE64_LENGTH = 1024
        const val MAX_SIGNATURE_BASE64_LENGTH = 512
    }
}
