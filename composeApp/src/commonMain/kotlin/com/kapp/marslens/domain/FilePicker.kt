package com.kapp.marslens.domain

interface FilePicker {
    suspend fun pickFile(): String?
    suspend fun pickDirectory(): String?
    suspend fun saveFile(defaultName: String): String?
}
