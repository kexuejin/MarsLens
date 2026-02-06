package com.kapp.marslens.data.parser

import com.kapp.marslens.data.model.LogEntry
import com.kapp.marslens.data.model.LogLevel
import com.kapp.marslens.domain.XlogDecoder
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class XlogParser : XlogDecoder {

    init {
        // Load the shared library.
        // The library name typically matches the name in Cargo.toml (with lib prefix on Unix)
        // e.g. libxlog_core.dylib / xlog_core.dll
        try {
            // Get the absolute path to the library to avoid java.library.path issues with Skiko
            val libName = if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
                "libxlog_core.dylib"
            } else {
                "xlog_core.dll"
            }
            // Check current directory to determine prefix
            val userDir = System.getProperty("user.dir")
            val path = if (userDir.endsWith("composeApp")) {
                "libs/$libName"
            } else {
                "composeApp/libs/$libName"
            }
            val libFile = java.io.File(userDir, path)
            println("Loading native library from: ${libFile.absolutePath}")
            System.load(libFile.absolutePath)
        } catch (e: UnsatisfiedLinkError) {
            println("Failed to load native library: ${e.message}")
        }
    }

    override suspend fun decode(filePath: String, privateKey: String?): List<LogEntry> {
        return try {
            decodeXlogNative(filePath, privateKey ?: "")
        } catch (e: Exception) {
            println("Native decode failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun scanDirectory(path: String): List<String> {
        return try {
            scanDirectoryNative(path)
        } catch (e: Exception) {
            println("Native scan failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun exportDecryptedFile(inputPath: String, outputPath: String, key: String?): Boolean {
        return try {
            exportDecryptedFileNative(inputPath, outputPath, key ?: "")
        } catch (e: Exception) {
            println("Native export failed: ${e.message}")
            false
        }
    }

    // External native methods implemented in Rust
    private external fun decodeXlogNative(path: String, key: String): List<LogEntry>

    private external fun scanDirectoryNative(path: String): List<String>
    
    private external fun exportDecryptedFileNative(path: String, output: String, key: String): Boolean
}
