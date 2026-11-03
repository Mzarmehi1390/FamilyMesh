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

@Dao
interface EnrollmentNonceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNonce(entity: ConsumedEnrollmentNonceEntity): Long

    @Query("DELETE FROM consumed_enrollment_nonces WHERE expiresAtEpochMillis <= :nowEpochMillis")
    suspend fun deleteExpiredNonces(nowEpochMillis: Long)

    @Transaction
    suspend fun consumeNonce(entity: ConsumedEnrollmentNonceEntity, nowEpochMillis: Long): Boolean {
        deleteExpiredNonces(nowEpochMillis)
        return insertNonce(entity) != -1L
    }
}

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
}

@Database(
    entities = [FamilySpaceEntity::class, FamilyMemberEntity::class, ConsumedEnrollmentNonceEntity::class],
    version = 2,
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

        fun create(context: Context): FamilyMeshDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FamilyMeshDatabase::class.java,
                "familymesh.db"
            ).addMigrations(MIGRATION_1_2).build()
    }
}
