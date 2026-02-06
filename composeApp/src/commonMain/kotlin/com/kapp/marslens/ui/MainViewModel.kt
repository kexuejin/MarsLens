package com.kapp.marslens.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapp.marslens.data.model.LogEntry
import com.kapp.marslens.data.model.LogLevel
import com.kapp.marslens.domain.FilePicker
import com.kapp.marslens.domain.XlogDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val filePath: String? = null,
    val isLoading: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val filterLevel: LogLevel = LogLevel.Verbose,
    val searchText: String = "",
    val decryptionKeys: List<String> = emptyList(),
    val selectedKey: String? = null,
    val error: String? = null
)

class MainViewModel(
    private val decoder: XlogDecoder,
    private val filePicker: FilePicker
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun pickFile() {
        viewModelScope.launch {
            val path = filePicker.pickFile()
            if (path != null) {
                loadFile(path)
            }
        }
    }

    fun pickDirectory(onDirectoryPicked: (String) -> Unit) {
        viewModelScope.launch {
            val path = filePicker.pickDirectory()
            if (path != null) {
                onDirectoryPicked(path)
            }
        }
    }

    fun addDecryptionKey(key: String) {
        _uiState.update { state ->
            if (key.isNotBlank() && !state.decryptionKeys.contains(key)) {
                state.copy(decryptionKeys = state.decryptionKeys + key, selectedKey = key)
            } else {
                state
            }
        }
    }

    fun removeDecryptionKey(key: String) {
        _uiState.update { state ->
            val updatedKeys = state.decryptionKeys.filter { it != key }
            state.copy(
                decryptionKeys = updatedKeys,
                selectedKey = if (state.selectedKey == key) updatedKeys.firstOrNull() else state.selectedKey
            )
        }
    }

    fun selectDecryptionKey(key: String?) {
        _uiState.update { it.copy(selectedKey = key) }
    }

    fun exportLogs() {
        val currentFile = _uiState.value.filePath ?: return
        viewModelScope.launch {
            val defaultName = currentFile.substringAfterLast("/").substringBeforeLast(".") + "_decrypted.txt"
            val outputPath = filePicker.saveFile(defaultName)
            if (outputPath != null) {
                _uiState.update { it.copy(isLoading = true) }
                val success = decoder.exportDecryptedFile(currentFile, outputPath, _uiState.value.selectedKey)
                _uiState.update { it.copy(isLoading = false, error = if (success) null else "Export failed") }
                if (success) {
                    println("Export successful: $outputPath")
                }
            }
        }
    }

    fun loadFile(path: String, key: String? = null) {
        viewModelScope.launch {
            val actualKey = key ?: _uiState.value.selectedKey
            _uiState.update { it.copy(isLoading = true, error = null, filePath = path, selectedKey = actualKey) }
            try {
                val logs = decoder.decode(path, actualKey)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        logs = logs,
                        filteredLogs = filterLogs(logs, it.filterLevel, it.searchText)
                    ) 
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun setFilterLevel(level: LogLevel) {
        _uiState.update { 
            it.copy(
                filterLevel = level,
                filteredLogs = filterLogs(it.logs, level, it.searchText)
            ) 
        }
    }

    fun setSearchText(text: String) {
        _uiState.update { 
            it.copy(
                searchText = text,
                filteredLogs = filterLogs(it.logs, it.filterLevel, text)
            ) 
        }
    }
    
    private fun filterLogs(logs: List<LogEntry>, level: LogLevel, search: String): List<LogEntry> {
        return logs.filter { entry ->
            val matchesLevel = if (level == LogLevel.Verbose) true else entry.level >= level
            val matchesSearch = if (search.isBlank()) true else {
                entry.message.contains(search, ignoreCase = true) || 
                entry.tag.contains(search, ignoreCase = true)
            }
            matchesLevel && matchesSearch
        }
    }
}
