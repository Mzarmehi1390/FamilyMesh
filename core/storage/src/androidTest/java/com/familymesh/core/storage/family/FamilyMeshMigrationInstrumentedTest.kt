package com.familymesh.core.storage.family

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyMeshMigrationInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val helper = MigrationTestHelper(
        instrumentation,
        FamilyMeshDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun everySupportedStartVersion_migratesToV7AndPreservesRowsAndConstraints() {
        for (startVersion in 1..7) {
            val databaseName = "familymesh-migration-v$startVersion.db"
            context.deleteDatabase(databaseName)

            helper.createDatabase(databaseName, startVersion).use { database ->
                seedRows(database, startVersion)
            }

            val migrated = helper.runMigrationsAndValidate(
                databaseName,
                7,
                true,
                *FamilyMeshDatabase.migrationsForTesting(),
            )
            try {
                assertEquals(1, count(migrated, "family_spaces"))
                assertEquals(1, count(migrated, "family_members"))
                assertEquals(1_000L, scalarLong(migrated, "SELECT createdAtEpochMillis FROM family_spaces WHERE id = 'family-$startVersion'"))
                assertEquals(if (startVersion >= 2) 1 else 0, count(migrated, "consumed_enrollment_nonces"))
                assertEquals(if (startVersion >= 3) 1 else 0, count(migrated, "pending_enrollment_requests"))
                assertEquals(if (startVersion >= 4) 1 else 0, count(migrated, "initial_owner_trust"))
                assertEquals(if (startVersion >= 5) 1 else 0, count(migrated, "trusted_initial_owners"))
                assertEquals(if (startVersion >= 6) 1 else 0, count(migrated, "enrollment_decisions"))
                assertEquals(if (startVersion >= 7) 1 else 0, count(migrated, "authenticated_family_devices"))
                assertFinalConstraints(migrated, startVersion)
            } finally {
                migrated.close()
                context.deleteDatabase(databaseName)
            }
        }
    }

    private fun seedRows(database: SupportSQLiteDatabase, version: Int) {
        database.execSQL(
            "INSERT INTO family_spaces (id, name, createdAtEpochMillis) VALUES (?, ?, ?)",
            arrayOf("family-$version", "Preserved family", 1_000L),
        )
        database.execSQL(
            "INSERT INTO family_members (id, familySpaceId, displayName, role, joinedAtEpochMillis) VALUES (?, ?, ?, ?, ?)",
            arrayOf("member-$version", "family-$version", "Preserved member", "OWNER", 1_100L),
        )
        if (version >= 2) {
            database.execSQL(
                "INSERT INTO consumed_enrollment_nonces VALUES (?, ?, ?, ?, ?)",
                arrayOf("nonce-$version", "family-$version", "nonce-request-$version", 1_200L, 9_000L),
            )
        }
        if (version >= 3) {
            database.execSQL(
                "INSERT INTO pending_enrollment_requests VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf("pending-$version", "family-$version", "device-$version", "MEMBER", 1_300L, 8_000L, 1_400L),
            )
        }
        if (version >= 4) {
            database.execSQL(
                "INSERT INTO initial_owner_trust VALUES (?, ?, ?, ?)",
                arrayOf("family-$version", "member-$version", "owner-device-$version", 1_500L),
            )
        }
        if (version >= 5) {
            database.execSQL(
                "INSERT INTO trusted_initial_owners VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf("family-$version", "member-$version", "trusted-device-$version", "cHVibGlj", 1_500L, 1_600L),
            )
        }
        if (version >= 6) {
            database.execSQL(
                "INSERT INTO enrollment_decisions VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("family-$version", "decision-$version", "applicant-$version", "owner-$version", "APPROVED", 1_700L, "c2ln", 1_800L),
            )
        }
        if (version >= 7) {
            database.execSQL(
                "INSERT INTO authenticated_family_devices VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf("family-$version", "authenticated-$version", "authenticated-device-$version", "cHVibGlj", "MEMBER", 1_900L, "c2ln"),
            )
        }
    }

    private fun assertFinalConstraints(database: SupportSQLiteDatabase, startVersion: Int) {
        // MigrationTestHelper exposes a raw SQLite connection. Enable FK enforcement
        // explicitly so the cascade checks exercise the same constraint behavior as Room.
        database.execSQL("PRAGMA foreign_keys = ON")
        assertEquals(
            "Foreign-key enforcement must be enabled during migration validation",
            1L,
            scalarLong(database, "PRAGMA foreign_keys"),
        )

        val expectedIndices = listOf(
            "index_family_members_familySpaceId",
            "index_pending_enrollment_requests_receivedAtEpochMillis",
            "index_pending_enrollment_requests_deviceKeyId",
            "index_initial_owner_trust_ownerDeviceKeyId",
            "index_trusted_initial_owners_ownerDeviceKeyId",
            "index_enrollment_decisions_ownerDeviceKeyId",
            "index_enrollment_decisions_recordedAtEpochMillis",
            "index_authenticated_family_devices_familySpaceId_deviceKeyId",
        )
        expectedIndices.forEach { indexName ->
            assertEquals(1, scalarLong(database, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = '$indexName'"))
        }

        val uniqueDeviceIndex = database.query("PRAGMA index_list('authenticated_family_devices')").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val unique = cursor.getInt(cursor.getColumnIndexOrThrow("unique")) == 1
                if (name == "index_authenticated_family_devices_familySpaceId_deviceKeyId" && unique) found = true
            }
            found
        }
        assertTrue("Authenticated device family/key index must remain unique", uniqueDeviceIndex)

        if (startVersion < 7) {
            database.execSQL(
                "INSERT INTO authenticated_family_devices VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf("family-$startVersion", "migration-test-device", "authenticated-device-$startVersion", "cHVibGlj", "MEMBER", 1_950L, "c2ln"),
            )
        }
        try {
            database.execSQL(
                "INSERT INTO authenticated_family_devices VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf("family-$startVersion", "duplicate-device", "authenticated-device-$startVersion", "cHVibGlj", "MEMBER", 2_000L, "c2ln"),
            )
            throw AssertionError("Expected unique family/device constraint to reject duplicate device.")
        } catch (_: SQLiteConstraintException) {
            // Expected: the unique composite index is enforced by SQLite.
        }

        database.execSQL("DELETE FROM family_spaces WHERE id LIKE 'family-%'")
        listOf(
            "family_members",
            "pending_enrollment_requests",
            "initial_owner_trust",
            "enrollment_decisions",
            "authenticated_family_devices",
        ).forEach { table ->
            assertEquals(
                "Deleting family-$startVersion must cascade to $table",
                0,
                count(database, table),
            )
        }
        // Historical v5 intentionally has no FK, so this row survives family deletion.
        assertEquals(if (startVersion >= 5) 1 else 0, count(database, "trusted_initial_owners"))
    }

    private fun count(database: SupportSQLiteDatabase, table: String): Int =
        scalarLong(database, "SELECT COUNT(*) FROM $table").toInt()

    private fun scalarLong(database: SupportSQLiteDatabase, sql: String): Long =
        database.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }
}
