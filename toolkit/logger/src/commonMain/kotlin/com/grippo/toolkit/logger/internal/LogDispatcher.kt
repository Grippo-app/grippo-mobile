package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.LogCategory
import com.grippo.toolkit.logger.internal.models.LogEntry

internal interface LogDestination {
    val location: String?
    fun write(entry: LogEntry)
}

internal class LogDispatcher(
    private val destination: LogDestination,
) {
    var listener: ((LogCategory, String) -> Unit)? = null

    val location: String?
        get() = destination.location

    fun dispatch(category: LogCategory, message: String) {
        val entry = LogEntry.create(category, message)
        destination.write(entry)
        listener?.invoke(category, message)
    }
}
