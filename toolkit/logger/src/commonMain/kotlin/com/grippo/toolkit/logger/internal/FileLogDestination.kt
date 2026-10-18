package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.LogEntry
import kotlin.coroutines.cancellation.CancellationException

internal class FileLogDestination(createWriter: () -> LogFileWriter) : LogDestination {

    private var writer: Lazy<LogFileWriter> = lazy(createWriter)

    override val location: String?
        get() = if (writer.isInitialized()) writer.value.location else null

    override fun write(entry: LogEntry) {
        try {
            val formatted = LogFormatter.format(entry)
            writer.value.append(formatted)
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            // A failed file log must not prevent reporting the original business error.
        }
    }

    fun updateWriter(newWriter: LogFileWriter) {
        writer = lazyOf(newWriter)
    }
}
