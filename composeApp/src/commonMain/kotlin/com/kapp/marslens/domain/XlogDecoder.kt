package com.kapp.marslens.domain

import com.kapp.marslens.data.model.LogEntry

interface XlogDecoder {
    suspend fun decode(filePath: String, privateKey: String?): List<LogEntry>
    suspend fun scanDirectory(path: String): List<String>
    suspend fun exportDecryptedFile(inputPath: String, outputPath: String, key: String?): Boolean
}
