package com.familymesh.core.common.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DeviceEnrollmentRequestTest {
    private fun request(
        status: EnrollmentRequestStatus = EnrollmentRequestStatus.PENDING,
    ) = DeviceEnrollmentRequest(
        id = "request-1",
        familySpaceId = "family-1",
        deviceKeyId = "device-key-1",
        requestedRole = FamilyRole.MEMBER,
        createdAtEpochMillis = 100L,
        expiresAtEpochMillis = 1_000L,
        status = status,
    )

    @Test
    fun validRequest_preservesFamilyDeviceAndRoleBinding() {
        val value = request()

        assertEquals("family-1", value.familySpaceId)
        assertEquals("device-key-1", value.deviceKeyId)
        assertEquals(FamilyRole.MEMBER, value.requestedRole)
        assertEquals(EnrollmentRequestStatus.PENDING, value.status)
    }

    @Test
    fun blankIdentifiers_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            request().copy(id = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DeviceEnrollmentRequest(
                id = "request",
                familySpaceId = " ",
                deviceKeyId = "key",
                requestedRole = FamilyRole.MEMBER,
                createdAtEpochMillis = 0L,
                expiresAtEpochMillis = 1L,
            )
        }
    }

    @Test
    fun expiryMustBeAfterCreation() {
        assertThrows(IllegalArgumentException::class.java) {
            request().copy(expiresAtEpochMillis = 100L)
        }
    }

    @Test
    fun pendingRequest_canBeApprovedBeforeExpiry() {
        assertEquals(
            EnrollmentRequestStatus.APPROVED,
            request().approve(999L).status,
        )
    }

    @Test
    fun pendingRequest_canBeRejectedBeforeExpiry() {
        assertEquals(
            EnrollmentRequestStatus.REJECTED,
            request().reject(500L).status,
        )
    }

    @Test
    fun pendingRequest_canBeRevokedBeforeExpiry() {
        assertEquals(
            EnrollmentRequestStatus.REVOKED,
            request().revoke(500L).status,
        )
    }

    @Test
    fun request_canExpireOnlyAtOrAfterExpiry() {
        assertThrows(IllegalArgumentException::class.java) {
            request().expire(999L)
        }
        assertEquals(
            EnrollmentRequestStatus.EXPIRED,
            request().expire(1_000L).status,
        )
    }

    @Test
    fun expiredRequestCannotBeApproved() {
        assertThrows(IllegalArgumentException::class.java) {
            request().approve(1_000L)
        }
    }

    @Test
    fun terminalRequestCannotTransitionAgain() {
        assertThrows(IllegalArgumentException::class.java) {
            request(EnrollmentRequestStatus.APPROVED).revoke(500L)
        }
    }

    @Test
    fun transitionCannotPrecedeCreation() {
        assertThrows(IllegalArgumentException::class.java) {
            request().approve(99L)
        }
    }
}
