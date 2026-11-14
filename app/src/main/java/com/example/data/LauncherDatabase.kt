package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "home_items")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val page: Int,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int,
    val type: String,
    val packageName: String? = null,
    val activityName: String? = null,
    val folderTitle: String? = null,
    val folderAppsCsv: String = "",
    val builtInWidgetType: String? = null,
    val appWidgetId: Int = -1,
    val widgetConfig: String? = null
)

@Entity(tableName = "app_customizations")
data class AppCustomizationEntity(
    @PrimaryKey val componentKey: String,
    val customLabel: String? = null,
    val customIconUri: String? = null,
    val customPackPackage: String? = null,
    val customPackDrawable: String? = null,
    val isHidden: Boolean = false,
    val launchCount: Int = 0
)

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface HomeItemDao {
    @Query("SELECT * FROM home_items ORDER BY page ASC, row ASC, col ASC")
    fun observeAll(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_items")
    suspend fun getAllOnce(): List<HomeItemEntity>

    @Query("SELECT COUNT(*) FROM home_items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HomeItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HomeItemEntity>)

    @Update
    suspend fun update(item: HomeItemEntity)

    @Query("DELETE FROM home_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM home_items")
    suspend fun deleteAll()
}

@Dao
interface AppCustomizationDao {
    @Query("SELECT * FROM app_customizations")
    fun observeAll(): Flow<List<AppCustomizationEntity>>

    @Query("SELECT * FROM app_customizations WHERE componentKey = :key LIMIT 1")
    suspend fun getByKey(key: String): AppCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppCustomizationEntity)

    @Query("UPDATE app_customizations SET launchCount = launchCount + 1 WHERE componentKey = :key")
    suspend fun incrementLaunchCount(key: String)
}

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes ORDER BY isDone ASC, createdAt DESC")
    fun observeAll(): Flow<List<QuickNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: QuickNoteEntity): Long

    @Update
    suspend fun update(note: QuickNoteEntity)

    @Query("DELETE FROM quick_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(
    entities = [HomeItemEntity::class, AppCustomizationEntity::class, QuickNoteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun homeItemDao(): HomeItemDao
    abstract fun appCustomizationDao(): AppCustomizationDao
    abstract fun quickNoteDao(): QuickNoteDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE home_items ADD COLUMN widgetConfig TEXT")
            }
        }

        fun getInstance(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "tilde_launcher.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
