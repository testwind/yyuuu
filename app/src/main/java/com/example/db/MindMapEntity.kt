package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mind_maps")
data class MindMapEntity(
    @PrimaryKey val id: String,
    val title: String,
    val rootNodeJson: String, // Stringified JSON representation of the central root mind map node
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncRevision: Int = 0,
    val sourcePath: String? = null // local or cloud file origin
)

@Entity(tableName = "sync_config")
data class SyncConfigEntity(
    @PrimaryKey val id: Int = 1,
    val type: String = "WebDAV",
    val serverUrl: String = "",
    val username: String = "",
    val token: String = "",
    val syncPath: String = "/FreeMapSync/",
    val lastSyncTime: Long = 0L,
    val isEnabled: Boolean = false
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // SUCCESS, ERROR, INFO
    val message: String
)
