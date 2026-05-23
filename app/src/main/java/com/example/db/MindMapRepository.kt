package com.example.db

import com.example.model.MindMapNode
import com.example.model.SyncConfig
import com.example.parser.FreeplaneParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MindMapRepository(private val mindMapDao: MindMapDao) {

    /**
     * Observable lists of saved mind maps.
     */
    val allMindMapsFlow: Flow<List<MindMapEntity>> = mindMapDao.getAllMindMapsFlow()

    /**
     * Auto-inject the rich, comprehensive Freeplane sample files if database is blank.
     */
    suspend fun checkAndPrepopulateSamples() {
        val existing = mindMapDao.getAllMindMaps()
        if (existing.isEmpty()) {
            val p1 = FreeplaneParser.parse(SampleMindMaps.PROJECT_PLAN_XML)
            if (p1 != null) {
                insertOrUpdateMindMap(p1, isSynced = false)
            }
            val p2 = FreeplaneParser.parse(SampleMindMaps.PERSONAL_GROWTH_XML)
            if (p2 != null) {
                insertOrUpdateMindMap(p2, isSynced = false)
            }
            val p3 = FreeplaneParser.parse(SampleMindMaps.STRATEGIC_MEETING_XML)
            if (p3 != null) {
                insertOrUpdateMindMap(p3, isSynced = false)
            }
            
            insertSyncLog("INFO", "🎁 首次启动：成功为您预置了 3 份经典 Freeplane 思维导图示例，支持展开/收起和结构化阅读！")
        }
    }

    /**
     * Get a reactive representation of a specific MindMapNode root.
     */
    fun getMindMapNodeFlow(id: String): Flow<MindMapNode?> {
        return mindMapDao.getMindMapFlowById(id).map { entity ->
            entity?.let { MindMapJsonSerializer.fromJson(it.rootNodeJson) }
        }
    }

    suspend fun getMindMapNodeById(id: String): MindMapNode? {
        val entity = mindMapDao.getMindMapById(id) ?: return null
        return MindMapJsonSerializer.fromJson(entity.rootNodeJson)
    }

    suspend fun getMindMapEntityById(id: String): MindMapEntity? {
        return mindMapDao.getMindMapById(id)
    }

    /**
     * Insert or update a MindMapNode to Room Database.
     */
    suspend fun insertOrUpdateMindMap(node: MindMapNode, isSynced: Boolean = false, sourcePath: String? = null) {
        val json = MindMapJsonSerializer.toJson(node)
        val entity = MindMapEntity(
            id = node.id,
            title = node.text,
            rootNodeJson = json,
            lastModified = System.currentTimeMillis(),
            isSynced = isSynced,
            sourcePath = sourcePath
        )
        mindMapDao.insertOrUpdateMindMap(entity)
    }

    suspend fun deleteMindMap(id: String) {
        mindMapDao.deleteMindMapById(id)
    }

    /**
     * Import a .mm XML file.
     */
    suspend fun importFromXml(xml: String, sourcePath: String? = null): MindMapNode? {
        val node = FreeplaneParser.parse(xml) ?: return null
        insertOrUpdateMindMap(node, isSynced = false, sourcePath = sourcePath)
        insertSyncLog("SUCCESS", "📥 成功导入外部思维导图：「${node.text}」- 包含 ${countNodes(node)} 个节点")
        return node
    }

    private fun countNodes(node: MindMapNode): Int {
        return 1 + node.children.sumOf { countNodes(it) }
    }

    // Sync Configurations
    val syncConfigFlow: Flow<SyncConfig> = mindMapDao.getSyncConfigFlow().map { entity ->
        entity?.let {
            SyncConfig(
                id = it.id,
                type = it.type,
                serverUrl = it.serverUrl,
                username = it.username,
                token = it.token,
                syncPath = it.syncPath,
                lastSyncTime = it.lastSyncTime,
                isEnabled = it.isEnabled
            )
        } ?: SyncConfig()
    }

    suspend fun getSyncConfig(): SyncConfig {
        val entity = mindMapDao.getSyncConfig() ?: return SyncConfig()
        return SyncConfig(
            id = entity.id,
            type = entity.type,
            serverUrl = entity.serverUrl,
            username = entity.username,
            token = entity.token,
            syncPath = entity.syncPath,
            lastSyncTime = entity.lastSyncTime,
            isEnabled = entity.isEnabled
        )
    }

    suspend fun saveSyncConfig(config: SyncConfig) {
        val entity = SyncConfigEntity(
            id = config.id,
            type = config.type,
            serverUrl = config.serverUrl,
            username = config.username,
            token = config.token,
            syncPath = config.syncPath,
            lastSyncTime = config.lastSyncTime,
            isEnabled = config.isEnabled
        )
        mindMapDao.saveSyncConfig(entity)
    }

    // Logs
    val syncLogsFlow: Flow<List<SyncLogEntity>> = mindMapDao.getSyncLogsFlow()

    suspend fun insertSyncLog(status: String, message: String) {
        mindMapDao.insertSyncLog(
            SyncLogEntity(
                status = status,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSyncLogs() {
        mindMapDao.clearSyncLogs()
    }
}
