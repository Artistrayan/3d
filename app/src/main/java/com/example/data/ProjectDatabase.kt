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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects_3d")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val sceneJson: String,
    val objectCount: Int,
    val vertexCount: Int,
    val triangleCount: Int,
    val accentHex: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "export_logs")
data class ExportLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val formatName: String,
    val byteSize: Int,
    val vertexCount: Int,
    val triangleCount: Int,
    val exportedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects_3d ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("DELETE FROM projects_3d WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM projects_3d")
    suspend fun getProjectCount(): Int

    @Query("SELECT * FROM export_logs ORDER BY exportedAt DESC LIMIT 25")
    fun getRecentExports(): Flow<List<ExportLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportLog(log: ExportLogEntity)
}

@Database(
    entities = [ProjectEntity::class, ExportLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PolyForgeDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: PolyForgeDatabase? = null

        fun getInstance(context: Context): PolyForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PolyForgeDatabase::class.java,
                    "polyforge_3d_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val recentExports: Flow<List<ExportLogEntity>> = projectDao.getRecentExports()

    suspend fun saveProject(project: ProjectEntity): Long = projectDao.insertProject(project)
    suspend fun deleteProject(id: Long) = projectDao.deleteProjectById(id)
    suspend fun getProjectCount(): Int = projectDao.getProjectCount()
    suspend fun recordExport(log: ExportLogEntity) = projectDao.insertExportLog(log)
}
