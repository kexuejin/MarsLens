package com.kapp.marslens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.kapp.marslens.ui.MainViewModel
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

@Composable
fun FileTreeSidebar(
    treeViewModel: FileTreeViewModel = koinViewModel(),
    mainViewModel: MainViewModel = koinViewModel(),
    onFileSelect: (String) -> Unit
) {
    val treeState by treeViewModel.uiState.collectAsState()
    val mainState by mainViewModel.uiState.collectAsState()
    var newKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(end = 1.dp)
    ) {
        // File Tree Area
        Box(modifier = Modifier.weight(1f)) {
            if (treeState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (treeState.nodes.isEmpty()) {
                SidebarEmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    items(flattenTree(treeState.nodes)) { node ->
                        FileTreeNodeItem(
                            node = node,
                            isSelected = node.path == treeState.selectedPath,
                            onToggle = { treeViewModel.toggleExpand(node.path) },
                            onClick = { 
                                if (!node.isDirectory) {
                                    treeViewModel.selectFile(node.path)
                                    onFileSelect(node.path)
                                } else {
                                    treeViewModel.toggleExpand(node.path)
                                }
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        // Decryption Keys Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(16.dp)
        ) {
            Text(
                "DECRYPTION KEYS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // List of keys
            if (mainState.decryptionKeys.isEmpty()) {
                Text(
                    "No keys added",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                mainState.decryptionKeys.forEach { key ->
                    KeyItem(
                        key = key,
                        isSelected = key == mainState.selectedKey,
                        onSelect = { mainViewModel.selectDecryptionKey(key) },
                        onRemove = { mainViewModel.removeDecryptionKey(key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Add Key Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    placeholder = { Text("Enter key...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    shape = MaterialTheme.shapes.small
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { 
                        if (newKey.isNotBlank()) {
                            mainViewModel.addDecryptionKey(newKey)
                            newKey = ""
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Add Key",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyItem(
    key: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onSelect,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = key.take(12) + if (key.length > 12) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "Remove Key",
                    modifier = Modifier.size(14.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FileTreeNodeItem(
    node: FileNode,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(vertical = 4.dp)
            .padding(start = (node.level * 16 + 8).dp), // Indentation
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/Collapse Icon or Spacer
        if (node.isDirectory) {
            Icon(
                if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp).clickable { onToggle() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))
        
        // File Icon
        Icon(
            if (node.isDirectory) {
                 if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
            } else {
                Icons.Default.Description
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (node.isDirectory) MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // File Name
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

// Helper to flatten the tree for LazyColumn
fun flattenTree(nodes: List<FileNode>): List<FileNode> {
    val result = mutableListOf<FileNode>()
    for (node in nodes) {
        result.add(node)
        if (node.isDirectory && node.isExpanded) {
            result.addAll(flattenTree(node.children))
        }
    }
    return result
}

@Composable
private fun SidebarEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No files found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "The directory is empty or cannot be accessed",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
