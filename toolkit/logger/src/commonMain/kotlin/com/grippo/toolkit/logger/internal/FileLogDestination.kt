package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.LogEntry

internal class FileLogDestination(initialWriter: LogFileWriter) : LogDestination {

    private var writer: LogFileWriter = initialWriter

    override val location: String
        get() = writer.location

    override fun write(entry: LogEntry) {
        val formatted = LogFormatter.format(entry)
        writer.append(formatted)
    }

    fun updateWriter(newWriter: LogFileWriter) {
        writer = newWriter
    }
}
