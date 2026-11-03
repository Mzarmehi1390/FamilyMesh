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
}

@Database(
    entities = [FamilySpaceEntity::class, FamilyMemberEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FamilyMeshDatabase : RoomDatabase() {
    abstract fun familySpaceDao(): FamilySpaceDao

    companion object {
        fun create(context: Context): FamilyMeshDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FamilyMeshDatabase::class.java,
                "familymesh.db"
            ).build()
    }
}
