package com.kapp.marslens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import com.kapp.marslens.ui.theme.*

@OptIn(KoinExperimentalAPI::class)
@Composable
fun LogViewerScreen() {
    val mainViewModel = koinViewModel<MainViewModel>()
    val mainState by mainViewModel.uiState.collectAsState()
    val treeViewModel = koinViewModel<FileTreeViewModel>()
    val treeState by treeViewModel.uiState.collectAsState()
    
    // Sync single file selection with sidebar tree
    LaunchedEffect(mainState.filePath) {
        mainState.filePath?.let { path ->
            treeViewModel.selectFileAndParent(path)
        }
    }
    var showKeyDialog by remember { mutableStateOf(false) }
    var privateKey by remember { mutableStateOf("") }

    if (showKeyDialog) {
        DecryptionDialog(
            onDismiss = { showKeyDialog = false },
            onConfirm = { key ->
                privateKey = key
                showKeyDialog = false
                // Re-load if file exists
                mainState.filePath?.let { mainViewModel.loadFile(it, key) }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Sidebar
        FileTreeSidebar(
            treeViewModel = treeViewModel,
            onFileSelect = { mainViewModel.loadFile(it) }
        )
        
        // Main Content
        Column(modifier = Modifier.fillMaxSize()) {
            GlassyToolbar(
                onOpenFile = { mainViewModel.pickFile() },
                onOpenFolder = { 
                    mainViewModel.pickDirectory { path ->
                         treeViewModel.loadDirectory(path)
                    }
                },
                searchText = mainState.searchText,
                onSearchChange = { mainViewModel.setSearchText(it) },
                currentFilter = mainState.filterLevel,
                onFilterChange = { mainViewModel.setFilterLevel(it) },
                onSetKey = { showKeyDialog = true },
                onExport = { mainViewModel.exportLogs() }
            )
            
            LogListHeader()
            
            LogList(
                logs = mainState.filteredLogs,
                isLoading = mainState.isLoading,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onOpenFile = { mainViewModel.pickFile() }
            )
            
            StatusBar(
                total = mainState.logs.size,
                filtered = mainState.filteredLogs.size,
                isLoading = mainState.isLoading,
                error = mainState.error
            )
        }
    }
}

// ... helper components ...

@Composable
fun DecryptionDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(400.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(24.dp)
    ) {
        Column {
            Text("Enter Private Key", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Private Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onConfirm(key) }) { Text("Apply") }
            }
        }
    }
}

@Composable
fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
             Box(modifier = Modifier.padding(24.dp)) {
                content()
             }
        }
    }
}

@Composable
fun GlassyToolbar(
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
    currentFilter: com.kapp.marslens.data.model.LogLevel,
    onFilterChange: (com.kapp.marslens.data.model.LogLevel) -> Unit,
    onSetKey: () -> Unit,
    onExport: () -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 1.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Search Bar
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.weight(1f).height(42.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchText,
                        onValueChange = onSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchText.isEmpty()) {
                                Text("Search logs (regex supported)...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f), style = MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Filter Dropdown
            Box {
                OutlinedButton(
                    onClick = { showFilterMenu = true },
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Level: ${currentFilter.name}", 
                        style = MaterialTheme.typography.bodySmall,
                        color = when(currentFilter) {
                            com.kapp.marslens.data.model.LogLevel.Verbose -> LevelVerbose
                            com.kapp.marslens.data.model.LogLevel.Debug -> LevelDebug
                            com.kapp.marslens.data.model.LogLevel.Info -> LevelInfo
                            com.kapp.marslens.data.model.LogLevel.Warning -> LevelWarn
                            com.kapp.marslens.data.model.LogLevel.Error -> LevelError
                            com.kapp.marslens.data.model.LogLevel.Fatal -> LevelFatal
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    com.kapp.marslens.data.model.LogLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    level.name, 
                                    color = when(level) {
                                        com.kapp.marslens.data.model.LogLevel.Verbose -> LevelVerbose
                                        com.kapp.marslens.data.model.LogLevel.Debug -> LevelDebug
                                        com.kapp.marslens.data.model.LogLevel.Info -> LevelInfo
                                        com.kapp.marslens.data.model.LogLevel.Warning -> LevelWarn
                                        com.kapp.marslens.data.model.LogLevel.Error -> LevelError
                                        com.kapp.marslens.data.model.LogLevel.Fatal -> LevelFatal
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                ) 
                            },
                            onClick = {
                                onFilterChange(level)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Actions
            IconButton(onClick = onSetKey) {
                 Icon(Icons.Filled.Lock, contentDescription = "Set Private Key", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            IconButton(onClick = onExport) {
                 Icon(Icons.Default.Upload, contentDescription = "Export", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onOpenFolder) {
                 Icon(Icons.Default.FolderOpen, contentDescription = "Open Folder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onOpenFile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open File", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun LogListHeader() {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha=0.9f),
        modifier = Modifier.padding(bottom = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 16.dp)) {
            HeaderCell("TIME", 110.dp)
            HeaderCell("PID", 55.dp)
            HeaderCell("TID", 55.dp)
            HeaderCell("LEVEL", 55.dp)
            HeaderCell("TAG", 140.dp)
            HeaderCell("MESSAGE", weight = 1f)
        }
    }
}

@Composable
fun RowScope.HeaderCell(text: String, width: androidx.compose.ui.unit.Dp? = null, weight: Float? = null) {
    val modifier = if (weight != null) Modifier.weight(weight) else Modifier.width(width!!)
    Text(
        text, 
        style = MaterialTheme.typography.labelSmall, 
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f),
        modifier = modifier
    )
}

@Composable
fun LogList(
    logs: List<com.kapp.marslens.data.model.LogEntry>, 
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onOpenFile: () -> Unit
) {
    var expandedIndex by remember { mutableStateOf(-1) }
    
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (logs.isEmpty() && !isLoading) {
            EmptyStateView(
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                onOpenFile = onOpenFile
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(logs.size) { index ->
                    LogItemRow(
                        item = logs[index],
                        isExpanded = expandedIndex == index,
                        onToggleExpand = {
                            expandedIndex = if (expandedIndex == index) -1 else index
                        }
                    )
                }
            }
        }
        
        if (isLoading) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun LogItemRow(
    item: com.kapp.marslens.data.model.LogEntry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val levelColor = when(item.level) {
        com.kapp.marslens.data.model.LogLevel.Verbose -> LevelVerbose
        com.kapp.marslens.data.model.LogLevel.Debug -> LevelDebug
        com.kapp.marslens.data.model.LogLevel.Info -> LevelInfo
        com.kapp.marslens.data.model.LogLevel.Warning -> LevelWarn
        com.kapp.marslens.data.model.LogLevel.Error -> LevelError
        com.kapp.marslens.data.model.LogLevel.Fatal -> LevelFatal
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val bgColor = if (item.level >= com.kapp.marslens.data.model.LogLevel.Warning) {
        levelColor.copy(alpha = 0.05f)
    } else {
        Color.Transparent
    }

    Surface(
        color = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else bgColor,
        onClick = onToggleExpand,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 8.dp), // Compact padding
                verticalAlignment = androidx.compose.ui.Alignment.Top
            ) {
                // Time
                Text(
                    item.timestamp.toString().substringAfter("T").take(12),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.width(110.dp)
                )
                // PID
                Text("${item.processId}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), modifier = Modifier.width(55.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                // TID
                Text("${item.threadId}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), modifier = Modifier.width(55.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                
                // Level
                Box(
                    modifier = Modifier.width(55.dp).padding(end = 8.dp),
                    contentAlignment = androidx.compose.ui.Alignment.CenterStart
                ) {
                    Text(
                        item.level.name.take(1), 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = levelColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                // Tag
                Text(
                    item.tag,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier.width(140.dp)
                )
                
                // Message (Short preview)
                Text(
                    item.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = if (item.level >= com.kapp.marslens.data.model.LogLevel.Error) levelColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = if (isExpanded) androidx.compose.ui.text.style.TextOverflow.Visible else androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            if (isExpanded) {
                // Expanded Details
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                            .padding(8.dp)
                    ) {
                         Row {
                             Text("Full Message:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                             Spacer(modifier = Modifier.weight(1f))
                             Text("Original Line:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                         }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        
                        item.originalLine?.let { line ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StatusBar(total: Int, filtered: Int, isLoading: Boolean, error: String?) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant, 
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Total: $total",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Filtered: $filtered",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Processing...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.primary)
            }
            
            error?.let {
                 Spacer(modifier = Modifier.width(16.dp))
                 Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                 Spacer(modifier = Modifier.width(4.dp))
                 Text(it, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    onOpenFile: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No logs loaded",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Open a .xlog file to start analyzing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onOpenFile,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open File")
        }
    }
}
