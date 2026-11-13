package com.familymesh.core.storage.family

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        context.deleteDatabase(DATABASE_NAME)
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
    fun consumeNonce_rollsBackNonceWhenPendingInsertFails() = runBlocking {
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))

        val retryableNonce = nonce(nonceDigest = "nonce-digest-2")
        try {
            dao.consumeNonce(
                retryableNonce,
                pending(requestId = REQUEST_ID, deviceKeyId = "different-device"),
                nowEpochMillis = 1_500L,
            )
            throw AssertionError("Expected duplicate pending-request insertion to fail.")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // The @Transaction must roll back the nonce insert as well.
        }

        assertTrue(
            dao.consumeNonce(
                retryableNonce,
                pending(requestId = "request-2", deviceKeyId = "device-2"),
                nowEpochMillis = 1_500L,
            )
        )
    }

    @Test
    fun databaseReopen_preservesPendingEnrollmentAndNonceReplayState() = runBlocking {
        database.close()
        context.deleteDatabase(DATABASE_NAME)
        database = Room.databaseBuilder(
            context,
            FamilyMeshDatabase::class.java,
            DATABASE_NAME,
        ).build()
        dao = database.enrollmentNonceDao()
        database.familySpaceDao().insertFamilySpace(
            FamilySpaceEntity(FAMILY_ID, "Test family", 1_000L)
        )
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))
        database.close()

        database = Room.databaseBuilder(
            context,
            FamilyMeshDatabase::class.java,
            DATABASE_NAME,
        ).build()
        dao = database.enrollmentNonceDao()

        assertEquals(pending(), dao.getPendingRequest(FAMILY_ID, REQUEST_ID))
        assertFalse(
            dao.consumeNonce(
                nonce(),
                pending(requestId = "request-2", deviceKeyId = "device-2"),
                nowEpochMillis = 1_500L,
            )
        )
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
    fun persistDecision_concurrentSameArtifactWithDifferentReceiptTimes_exposesCurrentConflict() = runBlocking {
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))

        // Characterization only: ADR 0014 leaves the desired receipt-time/idempotency
        // semantics open. This records the current DAO behavior without approving it.
        val firstReceipt = decision()
        val secondReceipt = firstReceipt.copy(recordedAtEpochMillis = 1_700L)
        val results = persistConcurrently(
            first = firstReceipt,
            firstNowEpochMillis = 1_600L,
            second = secondReceipt,
            secondNowEpochMillis = 1_700L,
        )

        assertEquals(1, results.count { it == "RECORDED" })
        assertEquals(1, results.count { it == "CONFLICT" })
        val stored = dao.getDecision(FAMILY_ID, REQUEST_ID)
        assertTrue(stored == firstReceipt || stored == secondReceipt)
        assertNull(dao.getPendingRequest(FAMILY_ID, REQUEST_ID))
    }

    @Test
    fun persistDecision_concurrentOppositeArtifacts_keepsSingleWinner() = runBlocking {
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))

        val approval = decision()
        val rejection = approval.copy(
            outcome = "REJECTED",
            decidedAtEpochMillis = 1_700L,
            signatureBase64 = "c2lnMg==",
            recordedAtEpochMillis = 1_700L,
        )
        val results = persistConcurrently(
            first = approval,
            firstNowEpochMillis = 1_600L,
            second = rejection,
            secondNowEpochMillis = 1_700L,
        )

        assertEquals(1, results.count { it == "RECORDED" })
        assertEquals(1, results.count { it == "CONFLICT" })
        val stored = dao.getDecision(FAMILY_ID, REQUEST_ID)
        assertTrue(stored == approval || stored == rejection)
        assertNull(dao.getPendingRequest(FAMILY_ID, REQUEST_ID))
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

    @Test
    fun sqliteConstraints_enforceDeviceUniquenessAndFamilyCascade() = runBlocking {
        val firstDevice = AuthenticatedFamilyDeviceEntity(
            familySpaceId = FAMILY_ID,
            requestId = "authenticated-request-1",
            deviceKeyId = "shared-device-key",
            publicKeyBase64 = "cHVibGlj",
            role = "MEMBER",
            joinedAtEpochMillis = 1_600L,
            acceptanceSignatureBase64 = "c2ln",
        )
        val secondDevice = firstDevice.copy(requestId = "authenticated-request-2")

        assertTrue(dao.insertAuthenticatedDevice(firstDevice) > 0L)
        assertEquals(-1L, dao.insertAuthenticatedDevice(secondDevice))
        assertTrue(dao.consumeNonce(nonce(), pending(), nowEpochMillis = 1_500L))

        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM family_spaces WHERE id = ?",
            arrayOf(FAMILY_ID),
        )

        assertEquals(0, rowCount("pending_enrollment_requests"))
        assertEquals(0, rowCount("authenticated_family_devices"))
    }

    private suspend fun persistConcurrently(
        first: EnrollmentDecisionEntity,
        firstNowEpochMillis: Long,
        second: EnrollmentDecisionEntity,
        secondNowEpochMillis: Long,
    ): List<String> = coroutineScope {
        val start = CompletableDeferred<Unit>()
        val firstResult = async(Dispatchers.IO) {
            start.await()
            dao.persistDecision(first, firstNowEpochMillis)
        }
        val secondResult = async(Dispatchers.IO) {
            start.await()
            dao.persistDecision(second, secondNowEpochMillis)
        }
        start.complete(Unit)
        listOf(firstResult.await(), secondResult.await())
    }

    private fun rowCount(tableName: String): Int {
        database.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM $tableName"
        ).use { cursor ->
            check(cursor.moveToFirst())
            return cursor.getInt(0)
        }
    }

    private fun nonce(nonceDigest: String = "nonce-digest") = ConsumedEnrollmentNonceEntity(
        nonceDigest = nonceDigest,
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
        const val DATABASE_NAME = "room-enrollment-reopen-test.db"
    }
}
