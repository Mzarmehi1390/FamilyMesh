package com.familymesh.core.storage.family

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnrollmentNonceDaoInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: FamilyMeshDatabase
    private lateinit var dao: EnrollmentNonceDao

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            FamilyMeshDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.enrollmentNonceDao()
        runBlocking {
            database.familySpaceDao().insertFamilySpace(
                FamilySpaceEntity(
                    id = FAMILY_ID,
                    name = "Test family",
                    createdAtEpochMillis = 1_000L,
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun consumeNonce_isAtomicAndRejectsReplay() = runBlocking {
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))
        assertFalse(
            dao.consumeNonce(
                nonce(),
                pending(requestId = "request-2", deviceKeyId = "device-2"),
                nowEpochMillis = 1_500L,
            )
        )

        assertEquals("request-1", dao.getPendingRequest(FAMILY_ID, "request-1")?.requestId)
        assertNull(dao.getPendingRequest(FAMILY_ID, "request-2"))
    }

    @Test
    fun persistDecision_recordsAndResolvesPendingRequestAtomically() = runBlocking {
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))

        val approval = decision()
        assertEquals("RECORDED", dao.persistDecision(approval, nowEpochMillis = 1_600L))
        assertEquals(approval, dao.getDecision(FAMILY_ID, REQUEST_ID))
        assertNull(dao.getPendingRequest(FAMILY_ID, REQUEST_ID))

        assertEquals(
            "ALREADY_RECORDED",
            dao.persistDecision(approval, nowEpochMillis = 1_600L),
        )
        assertEquals(
            "CONFLICT",
            dao.persistDecision(
                approval.copy(outcome = "REJECTED", signatureBase64 = "c2lnMg=="),
                nowEpochMillis = 1_600L,
            ),
        )
    }

    @Test
    fun finalizeAuthenticatedDevice_isIdempotentAndRejectsConflictingReplay() = runBlocking {
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))
        val device = AuthenticatedFamilyDeviceEntity(
            familySpaceId = FAMILY_ID,
            requestId = REQUEST_ID,
            deviceKeyId = "applicant-key",
            publicKeyBase64 = "cHVibGlj",
            role = "MEMBER",
            joinedAtEpochMillis = 1_600L,
            acceptanceSignatureBase64 = "c2ln",
        )

        assertEquals("FINALIZED", dao.finalizeAuthenticatedDevice(device))
        assertEquals(device, dao.getAuthenticatedDevice(FAMILY_ID, REQUEST_ID))
        assertNull(dao.getPendingRequest(FAMILY_ID, REQUEST_ID))
        assertEquals("ALREADY_FINALIZED", dao.finalizeAuthenticatedDevice(device))
        assertEquals(
            "CONFLICT",
            dao.finalizeAuthenticatedDevice(device.copy(acceptanceSignatureBase64 = "b3RoZXI=")),
        )
    }

    private fun nonce() = ConsumedEnrollmentNonceEntity(
        nonceDigest = "nonce-digest",
        familySpaceId = FAMILY_ID,
        requestId = REQUEST_ID,
        consumedAtEpochMillis = 1_500L,
        expiresAtEpochMillis = 9_000L,
    )

    private fun pending(
        requestId: String = REQUEST_ID,
        deviceKeyId: String = "applicant-key",
    ) = PendingEnrollmentRequestEntity(
        requestId = requestId,
        familySpaceId = FAMILY_ID,
        deviceKeyId = deviceKeyId,
        requestedRole = "MEMBER",
        createdAtEpochMillis = 1_000L,
        expiresAtEpochMillis = 8_000L,
        receivedAtEpochMillis = 1_500L,
    )

    private fun decision() = EnrollmentDecisionEntity(
        familySpaceId = FAMILY_ID,
        requestId = REQUEST_ID,
        applicantDeviceKeyId = "applicant-key",
        ownerDeviceKeyId = "owner-key",
        outcome = "APPROVED",
        decidedAtEpochMillis = 1_600L,
        signatureBase64 = "c2ln",
        recordedAtEpochMillis = 1_600L,
    )

    private companion object {
        const val FAMILY_ID = "family-1"
        const val REQUEST_ID = "request-1"
    }
}
