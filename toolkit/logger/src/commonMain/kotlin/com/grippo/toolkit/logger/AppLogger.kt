package com.grippo.toolkit.logger

import com.grippo.toolkit.logger.internal.FileLogDestination
import com.grippo.toolkit.logger.internal.LogDispatcher
import com.grippo.toolkit.logger.internal.LogFileReader
import com.grippo.toolkit.logger.internal.LogFileWriter
import com.grippo.toolkit.logger.internal.getCallerLocation
import com.grippo.toolkit.logger.internal.models.LogCategory

public object AppLogger {

    private val fileDestination = FileLogDestination(LogFileWriter.create())
    private val dispatcher = LogDispatcher(fileDestination)

    public fun logFileContentsByCategory(): Map<String, List<String>> {
        val location = dispatcher.location ?: return emptyMap()
        return LogFileReader.readGrouped(location)
            .mapKeys { (category, _) -> category.name }
    }

    public fun clearLogFile(): Boolean {
        val path = dispatcher.location
        val deleted = if (path != null) {
            LogFileWriter.deleteFile(path)
        } else {
            false
        }

        // Switch logger to a fresh empty file after cleanup
        fileDestination.updateWriter(LogFileWriter.create())

        return deleted
    }

    public object General {
        public fun error(msg: String): Unit = present(LogCategory.ERROR, "🔴 $msg")

        public fun warning(msg: String): Unit = present(LogCategory.WARNING, "⚠\uFE0F $msg")
    }

    public object Navigation {
        public fun log(msg: String): Unit = present(LogCategory.NAVIGATION, msg)
    }

    public object Network {
        public fun log(msg: String): Unit = present(LogCategory.NETWORK, msg)
    }

    public object Mapping {
        public fun <T> log(value: T?, msg: () -> String): T? {
            if (value != null) return value
            val location = getCallerLocation()
            present(LogCategory.MAPPING, "${msg()} $location")
            return null
        }
    }

    private fun present(category: LogCategory, msg: String) {
        onDebug {
            println(msg)
            dispatcher.dispatch(category, msg)
        }
    }

    private fun onDebug(action: () -> Unit) {
        action.invoke()
    }
}
