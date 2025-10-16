package com.grippo.toolkit.logger.internal.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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
