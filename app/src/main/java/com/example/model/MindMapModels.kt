package com.example.model

data class MindMapNode(
    val id: String,
    val text: String,
    val folded: Boolean = false,
    val children: List<MindMapNode> = emptyList(),
    val color: String? = null,
    val backgroundColor: String? = null,
    val note: String? = null,
    val side: String? = null, // "left" or "right" for root's subnodes
    val icons: List<String> = emptyList()
) {
    fun toggleFolded(targetId: String): MindMapNode {
        if (id == targetId) {
            return copy(folded = !folded)
        }
        return copy(children = children.map { it.toggleFolded(targetId) })
    }

    fun addChild(parentId: String, child: MindMapNode): MindMapNode {
        if (id == parentId) {
            return copy(children = children + child)
        }
        return copy(children = children.map { it.addChild(parentId, child) })
    }

    fun updateNode(targetId: String, newText: String, newColor: String? = null, newNote: String? = null): MindMapNode {
        if (id == targetId) {
            return copy(
                text = newText,
                color = if (newColor?.isBlank() == true) null else newColor,
                note = if (newNote?.isBlank() == true) null else newNote
            )
        }
        return copy(children = children.map { it.updateNode(targetId, newText, newColor, newNote) })
    }

    fun deleteNode(targetId: String): MindMapNode? {
        val updatedChildren = children.mapNotNull { it.deleteNode(targetId) }
        return copy(children = updatedChildren)
    }
}

data class SyncConfig(
    val id: Int = 1,
    val type: String = "WebDAV", // "WebDAV" or "Local" or "CloudPair"
    val serverUrl: String = "",
    val username: String = "",
    val token: String = "",
    val syncPath: String = "/FreeMapSync/",
    val lastSyncTime: Long = 0L,
    val isEnabled: Boolean = false
)

data class SyncLog(
    val timestamp: Long,
    val status: String, // "SUCCESS", "ERROR", "SYNCING"
    val message: String
)
