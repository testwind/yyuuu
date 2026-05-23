package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MindMapDao {

    @Query("SELECT * FROM mind_maps ORDER BY lastModified DESC")
    fun getAllMindMapsFlow(): Flow<List<MindMapEntity>>

    @Query("SELECT * FROM mind_maps ORDER BY lastModified DESC")
    suspend fun getAllMindMaps(): List<MindMapEntity>

    @Query("SELECT * FROM mind_maps WHERE id = :id LIMIT 1")
    fun getMindMapFlowById(id: String): Flow<MindMapEntity?>

    @Query("SELECT * FROM mind_maps WHERE id = :id LIMIT 1")
    suspend fun getMindMapById(id: String): MindMapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMindMap(mindMap: MindMapEntity)

    @Query("DELETE FROM mind_maps WHERE id = :id")
    suspend fun deleteMindMapById(id: String)

    // Sync Configurations
    @Query("SELECT * FROM sync_config WHERE id = 1 LIMIT 1")
    suspend fun getSyncConfig(): SyncConfigEntity?

    @Query("SELECT * FROM sync_config WHERE id = 1 LIMIT 1")
    fun getSyncConfigFlow(): Flow<SyncConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncConfig(config: SyncConfigEntity)

    // Sync Logs
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 50")
    fun getSyncLogsFlow(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    @Query("DELETE FROM sync_logs")
    suspend fun clearSyncLogs()
}
