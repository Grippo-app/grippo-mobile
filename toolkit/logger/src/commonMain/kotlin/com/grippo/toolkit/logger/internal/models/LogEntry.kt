package com.grippo.toolkit.logger.internal.models

import kotlin.time.Clock
import kotlin.time.Instant

internal data class LogEntry(
    val category: LogCategory,
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun create(category: LogCategory, message: String): LogEntry {
            return LogEntry(
                category = category,
                message = message,
                timestamp = Clock.System.now(),
            )
        }
    }
}
