package com.kapp.marslens.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

class DesktopFilePicker : FilePicker {
    override suspend fun pickFile(): String? = withContext(Dispatchers.Main) {
        val fileDialog = FileDialog(null as Frame?, "Select Xlog File", FileDialog.LOAD)
        fileDialog.isVisible = true
        if (fileDialog.directory != null && fileDialog.file != null) {
             fileDialog.directory + fileDialog.file
        } else {
            null
        }
    }

    override suspend fun pickDirectory(): String? = withContext(Dispatchers.Main) {
        // macOS hack for native directory picker
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        val fileDialog = FileDialog(null as Frame?, "Select Folder", FileDialog.LOAD)
        fileDialog.isVisible = true
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
        
        if (fileDialog.directory != null && fileDialog.file != null) {
            fileDialog.directory + fileDialog.file
        } else {
             null
        }
    }

    override suspend fun saveFile(defaultName: String): String? = withContext(Dispatchers.Main) {
        val fileDialog = FileDialog(null as Frame?, "Save Decrypted Log", FileDialog.SAVE)
        fileDialog.file = defaultName
        fileDialog.isVisible = true
        
        if (fileDialog.directory != null && fileDialog.file != null) {
            fileDialog.directory + fileDialog.file
        } else {
             null
        }
    }
}
