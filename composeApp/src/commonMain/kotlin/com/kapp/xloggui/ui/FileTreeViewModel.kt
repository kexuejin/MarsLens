package com.kapp.xloggui.ui

import com.kapp.xloggui.domain.XlogDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val level: Int,
    val children: List<FileNode> = emptyList(),
    var isExpanded: Boolean = false
)

data class FileTreeState(
    val rootPath: String? = null,
    val nodes: List<FileNode> = emptyList(),
    val selectedPath: String? = null,
    val isLoading: Boolean = false
)

class FileTreeViewModel(
    private val xlogDecoder: XlogDecoder
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileTreeState())
    val uiState: StateFlow<FileTreeState> = _uiState.asStateFlow()

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, rootPath = path)
            try {
                val files = xlogDecoder.scanDirectory(path).toSet()
                val tree = buildTree(path, files)
                _uiState.value = _uiState.value.copy(nodes = tree, isLoading = false)
            } catch (e: Exception) {
                println("Failed to load directory: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectFile(path: String) {
        _uiState.value = _uiState.value.copy(selectedPath = path)
    }

    fun selectFileAndParent(path: String) {
        viewModelScope.launch {
            val file = java.io.File(path)
            if (!file.exists()) return@launch
            
            val parentDir = file.parentFile ?: return@launch
            val parentPath = parentDir.absolutePath
            
            // Only reload if we are not already in this directory or a child of it
            // Simple logic: if rootPath is null or different from current parent, load it.
            if (_uiState.value.rootPath != parentPath) {
                _uiState.value = _uiState.value.copy(isLoading = true, rootPath = parentPath)
                try {
                    val files = xlogDecoder.scanDirectory(parentPath).toSet()
                    val tree = buildTree(parentPath, files)
                    _uiState.value = _uiState.value.copy(nodes = tree, isLoading = false, selectedPath = path)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(selectedPath = path)
            }
        }
    }

    fun toggleExpand(nodePath: String) {
        // Simple toggle logic for now - ideally we'd tracking expanded IDs
        // For this flat-node view needed for LazyColumn, we might need a flattener if list is huge
        // But for recursive tree view composable, state is often internal.
        // Let's implement a simple rebuild or state update if needed.
        // Actually, for a recursive UI, current expansion state is usually kept in UI or specific node wrapper.
        // Let's keep it simple: The UI will handle expansion state for now, or we update the node.
        
        // Re-creating tree with toggled state is expensive. 
        // We'll update the list in place if it was mutable, but it's immutable.
        // For V1 of tree, let's assume UI handles expansion state or we use a better tree library.
        // We'll implement a simple mutation for the demo.
        val newNodes = toggleNode(_uiState.value.nodes, nodePath)
        _uiState.value = _uiState.value.copy(nodes = newNodes)
    }

    private fun toggleNode(nodes: List<FileNode>, targetPath: String): List<FileNode> {
        return nodes.map { node ->
            if (node.path == targetPath) {
                node.copy(isExpanded = !node.isExpanded)
            } else {
                node.copy(children = toggleNode(node.children, targetPath))
            }
        }
    }

    private fun buildTree(rootPath: String, filePaths: Set<String>): List<FileNode> {
        // 1. Group by parent directory
        // 2. Build recursivley
        // This is a simplified builder. Better logic might be needed for complex trees.
        
        // Root node
        val rootName = rootPath.substringAfterLast('/').ifEmpty { rootPath }
        
        // We need to construct the full hierarchy.
        // Map paths to relative components
        val rootFile = java.io.File(rootPath)
        
        return listOfNotNull(buildNode(rootFile, filePaths, 0))
    }

    private fun buildNode(directory: java.io.File, allFiles: Set<String>, level: Int): FileNode? {
        if (level > 20) return null // Safety depth limit
        
        // Prevent infinite loops with symlinks
        val path = try { directory.canonicalPath } catch (e: Exception) { directory.absolutePath }
        
        val children = try {
            directory.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                ?.mapNotNull { file ->
                    if (file.isDirectory) {
                        buildNode(file, allFiles, level + 1)
                    } else {
                        val ext = file.extension.lowercase()
                        val validExt = ext in listOf("xlog", "mmap", "mmap2", "mmap3")
                        if (validExt && allFiles.contains(file.absolutePath)) {
                            FileNode(file.name, file.absolutePath, false, level + 1, emptyList())
                        } else {
                            null
                        }
                    }
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // Only return a directory node if it has children or if it's the root 
        // (but for recursive calls, we only want directories with content)
        if (children.isEmpty() && level > 0) return null

        return FileNode(
            name = if (level == 0) directory.absolutePath else directory.name,
            path = directory.absolutePath,
            isDirectory = true,
            level = level,
            children = children,
            isExpanded = level < 1 // Auto expand root
        )
    }
}
