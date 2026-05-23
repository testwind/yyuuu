package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.MindMapDatabase
import com.example.db.MindMapEntity
import com.example.db.MindMapRepository
import com.example.db.SyncLogEntity
import com.example.model.MindMapNode
import com.example.model.SyncConfig
import com.example.parser.FreeplaneParser
import com.example.sync.SyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MindMapViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MindMapDatabase.getDatabase(application)
    private val repository = MindMapRepository(database.mindMapDao())
    private val syncManager = SyncManager(repository)

    // Dashboard State
    val allMindMaps: StateFlow<List<MindMapEntity>> = repository.allMindMapsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Editor State
    private val _activeMindMapId = MutableStateFlow<String?>(null)
    val activeMindMapId: StateFlow<String?> = _activeMindMapId.asStateFlow()

    private val _activeRootNode = MutableStateFlow<MindMapNode?>(null)
    val activeRootNode: StateFlow<MindMapNode?> = _activeRootNode.asStateFlow()

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    private val _selectedNode = MutableStateFlow<MindMapNode?>(null)
    val selectedNode: StateFlow<MindMapNode?> = _selectedNode.asStateFlow()

    // Sync state
    val syncConfig: StateFlow<SyncConfig> = repository.syncConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncConfig())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.syncLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pairingCode = MutableStateFlow("")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    init {
        viewModelScope.launch {
            // Check and load prebuilt samples on start
            repository.checkAndPrepopulateSamples()
            regeneratePairingCode()
        }
    }

    /**
     * Set the currently active mind map to load and read.
     */
    fun selectMindMap(id: String?) {
        _activeMindMapId.value = id
        _selectedNode.value = null
        resetZoomAndPan()
        if (id == null) {
            _activeRootNode.value = null
            return
        }
        viewModelScope.launch {
            val node = repository.getMindMapNodeById(id)
            _activeRootNode.value = node
        }
    }

    fun resetZoomAndPan() {
        _zoomScale.value = 1f
        _panOffset.value = Offset.Zero
    }

    fun adjustZoom(factor: Float) {
        _zoomScale.value = (_zoomScale.value * factor).coerceIn(0.2f, 3.0f)
    }

    fun updatePan(offset: Offset) {
        _panOffset.value = _panOffset.value + offset
    }

    fun selectNode(node: MindMapNode?) {
        _selectedNode.value = node
    }

    /**
     * Toggles node folding/collapsing state reactively.
     */
    fun toggleNodeFolded(nodeId: String) {
        val currentRoot = _activeRootNode.value ?: return
        val updated = currentRoot.toggleFolded(nodeId)
        _activeRootNode.value = updated
        
        // Save back to database silently
        viewModelScope.launch {
            repository.insertOrUpdateMindMap(updated, isSynced = false)
            // If the selected node was updated, refresh its details reference
            if (_selectedNode.value?.id == nodeId) {
                _selectedNode.value = findNodeById(updated, nodeId)
            }
        }
    }

    /**
     * Adds a child node beneath a specified parent node in the active tree.
     */
    fun addChildNode(parentId: String, text: String, color: String? = null, side: String? = null) {
        val currentRoot = _activeRootNode.value ?: return
        val newChild = MindMapNode(
            id = "node_${UUID.randomUUID()}",
            text = text,
            color = color,
            side = side ?: findNodeById(currentRoot, parentId)?.side
        )
        val updated = currentRoot.addChild(parentId, newChild)
        _activeRootNode.value = updated
        
        viewModelScope.launch {
            repository.insertOrUpdateMindMap(updated, isSynced = false)
        }
    }

    /**
     * Updates an existing node's elements (Text, Color, Notes) in the active tree.
     */
    fun updateNodeDetails(nodeId: String, newText: String, newColor: String? = null, newNote: String? = null) {
        val currentRoot = _activeRootNode.value ?: return
        val updated = currentRoot.updateNode(nodeId, newText, newColor, newNote)
        _activeRootNode.value = updated
        
        viewModelScope.launch {
            repository.insertOrUpdateMindMap(updated, isSynced = false)
            if (_selectedNode.value?.id == nodeId) {
                _selectedNode.value = findNodeById(updated, nodeId)
            }
        }
    }

    /**
     * Deletes a node and all of its descendants from the active tree.
     */
    fun deleteNode(nodeId: String) {
        val currentRoot = _activeRootNode.value ?: return
        
        // Prevent deleting the central root node
        if (currentRoot.id == nodeId) {
            return
        }
        
        val updated = currentRoot.deleteNode(nodeId) ?: return
        _activeRootNode.value = updated
        
        if (_selectedNode.value?.id == nodeId) {
            _selectedNode.value = null
        }
        
        viewModelScope.launch {
            repository.insertOrUpdateMindMap(updated, isSynced = false)
        }
    }

    /**
     * Deletes a mind map completely from local database.
     */
    fun deleteMindMap(id: String) {
        viewModelScope.launch {
            repository.deleteMindMap(id)
        }
    }

    /**
     * Create a brand-new blank Freeplane mind map diagram.
     */
    fun createNewMindMap(title: String) {
        val newRoot = MindMapNode(
            id = "root_${UUID.randomUUID()}",
            text = title,
            folded = false,
            color = "#1A73E8"
        )
        viewModelScope.launch {
            repository.insertOrUpdateMindMap(newRoot, isSynced = false)
            selectMindMap(newRoot.id)
        }
    }

    /**
     * Import raw XML mind map file.
     */
    fun importFromXmlText(xmlText: String): Boolean {
        var success = false
        viewModelScope.launch {
            val node = repository.importFromXml(xmlText)
            if (node != null) {
                success = true
                selectMindMap(node.id)
            }
        }
        return success
    }

    /**
     * Format the active mind map back into standard Freeplane XML content for file sharing.
     */
    fun getActiveMindMapXml(): String {
        return _activeRootNode.value?.let { root ->
            FreeplaneParser.toXml(root)
        } ?: ""
    }

    // Sync Controls
    fun triggerSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            syncManager.syncAll(isManual = true)
            _isSyncing.value = false
        }
    }

    fun saveSyncSettings(serverUrl: String, username: String, token: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSyncConfig()
            val updated = current.copy(
                serverUrl = serverUrl,
                username = username,
                token = token,
                isEnabled = isEnabled
            )
            repository.saveSyncConfig(updated)
            repository.insertSyncLog("INFO", "⚙️ 同步配置已成功保存！当前云同步状态：「${if (isEnabled) "已启用" else "已禁用"}」")
        }
    }

    fun clearLog() {
        viewModelScope.launch {
            repository.clearSyncLogs()
        }
    }

    fun regeneratePairingCode() {
        val code1 = (100..999).random()
        val code2 = (100..999).random()
        _pairingCode.value = "$code1 $code2"
    }

    // Binary search tree helper
    private fun findNodeById(root: MindMapNode, id: String): MindMapNode? {
        if (root.id == id) return root
        for (child in root.children) {
            val found = findNodeById(child, id)
            if (found != null) return found
        }
        return null
    }
}
