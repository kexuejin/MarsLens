package com.kapp.xloggui.data.model

import kotlin.time.ExperimentalTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestamp: LocalDateTime,
    val threadId: Long,
    val processId: Long,
    val originalLine: String? = null // For debugging or copy
) {
    companion object {
        @OptIn(ExperimentalTime::class)
        @JvmStatic
        fun createFromNative(
            levelInt: Int,
            tag: String,
            message: String,
            timestampMs: Long,
            threadId: Long,
            processId: Long
        ): LogEntry {
            val level = LogLevel.entries.getOrElse(levelInt) { LogLevel.Info }
            val time = Instant.fromEpochMilliseconds(timestampMs).toLocalDateTime(TimeZone.currentSystemDefault())
            return LogEntry(level, tag, message, time, threadId, processId)
        }
    }
}

enum class LogLevel {
    Verbose, Debug, Info, Warning, Error, Fatal, None
}
