package com.familymesh.core.storage.family

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "family_spaces")
data class FamilySpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "family_members",
    foreignKeys = [
        ForeignKey(
            entity = FamilySpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["familySpaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["familySpaceId"])]
)
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val familySpaceId: String,
    val displayName: String,
    val role: String,
    val joinedAtEpochMillis: Long
)

@Entity(tableName = "consumed_enrollment_nonces")
data class ConsumedEnrollmentNonceEntity(
    @PrimaryKey val nonceDigest: String,
    val familySpaceId: String,
    val requestId: String,
    val consumedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
)

@Entity(
    tableName = "pending_enrollment_requests",
    primaryKeys = ["familySpaceId", "requestId"],
    foreignKeys = [ForeignKey(entity = FamilySpaceEntity::class, parentColumns = ["id"], childColumns = ["familySpaceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["receivedAtEpochMillis"]), Index(value = ["deviceKeyId"])]
)
data class PendingEnrollmentRequestEntity(
    val requestId: String,
    val familySpaceId: String,
    val deviceKeyId: String,
    val requestedRole: String,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
)

@Entity(
    tableName = "enrollment_decisions",
    primaryKeys = ["familySpaceId", "requestId"],
    foreignKeys = [ForeignKey(entity = FamilySpaceEntity::class, parentColumns = ["id"], childColumns = ["familySpaceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["ownerDeviceKeyId"]), Index(value = ["recordedAtEpochMillis"])]
)
data class EnrollmentDecisionEntity(
    val familySpaceId: String,
    val requestId: String,
    val applicantDeviceKeyId: String,
    val ownerDeviceKeyId: String,
    val outcome: String,
    val decidedAtEpochMillis: Long,
    val signatureBase64: String,
    val recordedAtEpochMillis: Long,
)

@Entity(
    tableName = "authenticated_family_devices",
    primaryKeys = ["familySpaceId", "requestId"],
    foreignKeys = [ForeignKey(entity = FamilySpaceEntity::class, parentColumns = ["id"], childColumns = ["familySpaceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["familySpaceId", "deviceKeyId"], unique = true)]
)
data class AuthenticatedFamilyDeviceEntity(
    val familySpaceId: String,
    val requestId: String,
    val deviceKeyId: String,
    val publicKeyBase64: String,
    val role: String,
    val joinedAtEpochMillis: Long,
    val acceptanceSignatureBase64: String,
)

@Dao
interface EnrollmentNonceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNonce(entity: ConsumedEnrollmentNonceEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPendingRequest(entity: PendingEnrollmentRequestEntity)

    @Query("SELECT * FROM pending_enrollment_requests ORDER BY receivedAtEpochMillis DESC, requestId ASC")
    fun observePendingRequests(): Flow<List<PendingEnrollmentRequestEntity>>

    @Query("SELECT * FROM pending_enrollment_requests WHERE familySpaceId = :familySpaceId AND requestId = :requestId LIMIT 1")
    suspend fun getPendingRequest(familySpaceId: String, requestId: String): PendingEnrollmentRequestEntity?

    @Query("DELETE FROM pending_enrollment_requests WHERE familySpaceId = :familySpaceId AND requestId = :requestId")
    suspend fun deletePendingRequest(familySpaceId: String, requestId: String): Int

    @Query("SELECT * FROM authenticated_family_devices WHERE familySpaceId = :familySpaceId AND requestId = :requestId LIMIT 1")
    suspend fun getAuthenticatedDevice(familySpaceId: String, requestId: String): AuthenticatedFamilyDeviceEntity?

    @Query("SELECT * FROM authenticated_family_devices WHERE familySpaceId = :familySpaceId ORDER BY joinedAtEpochMillis ASC, requestId ASC")
    fun observeAuthenticatedDevices(familySpaceId: String): Flow<List<AuthenticatedFamilyDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAuthenticatedDevice(entity: AuthenticatedFamilyDeviceEntity): Long

    @Transaction
    suspend fun finalizeAuthenticatedDevice(device: AuthenticatedFamilyDeviceEntity): String {
        val existing = getAuthenticatedDevice(device.familySpaceId, device.requestId)
        if (existing != null) return if (existing == device) "ALREADY_FINALIZED" else "CONFLICT"
        val pending = getPendingRequest(device.familySpaceId, device.requestId)
            ?: return "REQUEST_NOT_PENDING"
        if (pending.deviceKeyId != device.deviceKeyId ||
            pending.requestedRole != device.role ||
            device.joinedAtEpochMillis < pending.createdAtEpochMillis ||
            device.joinedAtEpochMillis >= pending.expiresAtEpochMillis
        ) return "CONFLICT"
        if (insertAuthenticatedDevice(device) == -1L) return "CONFLICT"
        if (deletePendingRequest(device.familySpaceId, device.requestId) != 1) {
            throw IllegalStateException("Pending request changed during finalization.")
        }
        return "FINALIZED"
    }

    @Query("SELECT * FROM enrollment_decisions ORDER BY recordedAtEpochMillis DESC, requestId ASC")
    fun observeDecisions(): Flow<List<EnrollmentDecisionEntity>>

    @Query("SELECT * FROM enrollment_decisions WHERE familySpaceId = :familySpaceId AND requestId = :requestId LIMIT 1")
    suspend fun getDecision(familySpaceId: String, requestId: String): EnrollmentDecisionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDecision(entity: EnrollmentDecisionEntity): Long

    @Transaction
    suspend fun persistDecision(
        decision: EnrollmentDecisionEntity,
        nowEpochMillis: Long,
    ): String {
        val existing = getDecision(decision.familySpaceId, decision.requestId)
        if (existing != null) {
            return if (existing == decision) "ALREADY_RECORDED" else "CONFLICT"
        }
        val pending = getPendingRequest(decision.familySpaceId, decision.requestId)
            ?: return "REQUEST_NOT_PENDING"
        if (pending.deviceKeyId != decision.applicantDeviceKeyId ||
            decision.decidedAtEpochMillis < pending.createdAtEpochMillis ||
            decision.decidedAtEpochMillis >= pending.expiresAtEpochMillis ||
            decision.recordedAtEpochMillis != nowEpochMillis
        ) return "CONFLICT"
        if (insertDecision(decision) == -1L) return "CONFLICT"
        if (deletePendingRequest(decision.familySpaceId, decision.requestId) != 1) {
            throw IllegalStateException("Pending request changed during decision transaction.")
        }
        return "RECORDED"
    }

    @Query("DELETE FROM consumed_enrollment_nonces WHERE expiresAtEpochMillis <= :nowEpochMillis")
    suspend fun deleteExpiredNonces(nowEpochMillis: Long)

    @Transaction
    suspend fun consumeNonce(
        nonce: ConsumedEnrollmentNonceEntity,
        pending: PendingEnrollmentRequestEntity,
        nowEpochMillis: Long,
    ): Boolean {
        deleteExpiredNonces(nowEpochMillis)
        if (insertNonce(nonce) == -1L) return false
        insertPendingRequest(pending)
        return true
    }
}

@Entity(
    tableName = "initial_owner_trust",
    primaryKeys = ["familySpaceId"],
    foreignKeys = [ForeignKey(entity = FamilySpaceEntity::class, parentColumns = ["id"], childColumns = ["familySpaceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["ownerDeviceKeyId"])]
)
data class InitialOwnerTrustEntity(
    val familySpaceId: String,
    val ownerMemberId: String,
    val ownerDeviceKeyId: String,
    val establishedAtEpochMillis: Long,
)

@Entity(tableName = "trusted_initial_owners", primaryKeys = ["familySpaceId"], indices = [Index(value = ["ownerDeviceKeyId"])])
data class TrustedInitialOwnerEntity(
    val familySpaceId: String,
    val ownerMemberId: String,
    val ownerDeviceKeyId: String,
    val ownerPublicKeyBase64: String,
    val establishedAtEpochMillis: Long,
    val pinnedAtEpochMillis: Long,
)

@Dao
interface FamilySpaceDao {
    @Query("SELECT * FROM family_spaces ORDER BY createdAtEpochMillis DESC, id ASC")
    fun observeFamilySpaces(): Flow<List<FamilySpaceEntity>>

    @Query(
        "SELECT * FROM family_members " +
            "WHERE familySpaceId = :familySpaceId " +
            "ORDER BY joinedAtEpochMillis ASC, id ASC"
    )
    fun observeMembers(familySpaceId: String): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFamilySpace(entity: FamilySpaceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFamilyMember(entity: FamilyMemberEntity)

    @Query("SELECT * FROM initial_owner_trust ORDER BY establishedAtEpochMillis ASC, familySpaceId ASC")
    fun observeInitialOwners(): Flow<List<InitialOwnerTrustEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialOwnerTrust(entity: InitialOwnerTrustEntity): Long

    @Transaction
    suspend fun establishInitialOwner(trust: InitialOwnerTrustEntity, owner: FamilyMemberEntity): Boolean {
        if (insertInitialOwnerTrust(trust) == -1L) return false
        insertFamilyMember(owner)
        return true
    }

    @Query("SELECT * FROM trusted_initial_owners ORDER BY pinnedAtEpochMillis ASC, familySpaceId ASC")
    fun observeTrustedOwners(): Flow<List<TrustedInitialOwnerEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrustedOwner(entity: TrustedInitialOwnerEntity): Long

    @Query("SELECT * FROM trusted_initial_owners WHERE familySpaceId = :familySpaceId LIMIT 1")
    suspend fun getTrustedOwner(familySpaceId: String): TrustedInitialOwnerEntity?
}

@Database(
    entities = [FamilySpaceEntity::class, FamilyMemberEntity::class, ConsumedEnrollmentNonceEntity::class, PendingEnrollmentRequestEntity::class, InitialOwnerTrustEntity::class, TrustedInitialOwnerEntity::class, EnrollmentDecisionEntity::class, AuthenticatedFamilyDeviceEntity::class],
    version = 7,
    exportSchema = true
)
abstract class FamilyMeshDatabase : RoomDatabase() {
    abstract fun familySpaceDao(): FamilySpaceDao
    abstract fun enrollmentNonceDao(): EnrollmentNonceDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS consumed_enrollment_nonces (" +
                        "nonceDigest TEXT NOT NULL PRIMARY KEY, " +
                        "familySpaceId TEXT NOT NULL, " +
                        "requestId TEXT NOT NULL, " +
                        "consumedAtEpochMillis INTEGER NOT NULL, " +
                        "expiresAtEpochMillis INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS pending_enrollment_requests (" +
                        "requestId TEXT NOT NULL, familySpaceId TEXT NOT NULL, deviceKeyId TEXT NOT NULL, " +
                        "requestedRole TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, " +
                        "expiresAtEpochMillis INTEGER NOT NULL, receivedAtEpochMillis INTEGER NOT NULL, " +
                        "PRIMARY KEY(familySpaceId, requestId), " +
                        "FOREIGN KEY(familySpaceId) REFERENCES family_spaces(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pending_enrollment_requests_receivedAtEpochMillis ON pending_enrollment_requests(receivedAtEpochMillis)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pending_enrollment_requests_deviceKeyId ON pending_enrollment_requests(deviceKeyId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS initial_owner_trust (" +
                        "familySpaceId TEXT NOT NULL PRIMARY KEY, ownerMemberId TEXT NOT NULL, " +
                        "ownerDeviceKeyId TEXT NOT NULL, establishedAtEpochMillis INTEGER NOT NULL, " +
                        "FOREIGN KEY(familySpaceId) REFERENCES family_spaces(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_initial_owner_trust_ownerDeviceKeyId ON initial_owner_trust(ownerDeviceKeyId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS enrollment_decisions (" +
                        "familySpaceId TEXT NOT NULL, requestId TEXT NOT NULL, applicantDeviceKeyId TEXT NOT NULL, " +
                        "ownerDeviceKeyId TEXT NOT NULL, outcome TEXT NOT NULL, decidedAtEpochMillis INTEGER NOT NULL, " +
                        "signatureBase64 TEXT NOT NULL, recordedAtEpochMillis INTEGER NOT NULL, " +
                        "PRIMARY KEY(familySpaceId, requestId), " +
                        "FOREIGN KEY(familySpaceId) REFERENCES family_spaces(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_enrollment_decisions_ownerDeviceKeyId ON enrollment_decisions(ownerDeviceKeyId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_enrollment_decisions_recordedAtEpochMillis ON enrollment_decisions(recordedAtEpochMillis)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS trusted_initial_owners (familySpaceId TEXT NOT NULL PRIMARY KEY, ownerMemberId TEXT NOT NULL, ownerDeviceKeyId TEXT NOT NULL, ownerPublicKeyBase64 TEXT NOT NULL, establishedAtEpochMillis INTEGER NOT NULL, pinnedAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_trusted_initial_owners_ownerDeviceKeyId ON trusted_initial_owners(ownerDeviceKeyId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS authenticated_family_devices (" +
                        "familySpaceId TEXT NOT NULL, requestId TEXT NOT NULL, deviceKeyId TEXT NOT NULL, " +
                        "publicKeyBase64 TEXT NOT NULL, role TEXT NOT NULL, joinedAtEpochMillis INTEGER NOT NULL, " +
                        "acceptanceSignatureBase64 TEXT NOT NULL, PRIMARY KEY(familySpaceId, requestId), " +
                        "FOREIGN KEY(familySpaceId) REFERENCES family_spaces(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_authenticated_family_devices_familySpaceId_deviceKeyId ON authenticated_family_devices(familySpaceId, deviceKeyId)")
            }
        }

        internal fun migrationsForTesting(): Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
        )

        fun create(context: Context): FamilyMeshDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FamilyMeshDatabase::class.java,
                "familymesh.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()
    }
}
