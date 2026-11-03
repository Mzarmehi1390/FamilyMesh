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

@Dao
interface EnrollmentNonceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNonce(entity: ConsumedEnrollmentNonceEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPendingRequest(entity: PendingEnrollmentRequestEntity)

    @Query("SELECT * FROM pending_enrollment_requests ORDER BY receivedAtEpochMillis DESC, requestId ASC")
    fun observePendingRequests(): Flow<List<PendingEnrollmentRequestEntity>>

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
}

@Database(
    entities = [FamilySpaceEntity::class, FamilyMemberEntity::class, ConsumedEnrollmentNonceEntity::class, PendingEnrollmentRequestEntity::class, InitialOwnerTrustEntity::class],
    version = 4,
    exportSchema = false
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

        fun create(context: Context): FamilyMeshDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FamilyMeshDatabase::class.java,
                "familymesh.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
}
