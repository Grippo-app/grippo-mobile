package com.grippo.toolkit.logger

import com.grippo.toolkit.logger.internal.FileLogDestination
import com.grippo.toolkit.logger.internal.LogDispatcher
import com.grippo.toolkit.logger.internal.LogFileReader
import com.grippo.toolkit.logger.internal.LogFileWriter
import com.grippo.toolkit.logger.internal.PerformanceTracker
import com.grippo.toolkit.logger.internal.getCallerLocation
import com.grippo.toolkit.logger.internal.models.LogCategory

public object AppLogger {

    private val fileDestination = FileLogDestination(LogFileWriter.create())
    private val dispatcher = LogDispatcher(fileDestination)
//
//    init {
//        configure()
//    }
//
//    public fun configure(fileName: String = LogFileWriter.DEFAULT_FILE_NAME) {
//        fileDestination.updateWriter(LogFileWriter.create(fileName))
//    }

    public fun logFileContents(): String {
        val location = dispatcher.location ?: return ""
        return LogFileReader.read(location)
    }

    public object General {
        public fun error(msg: String): Unit = present(LogCategory.GENERAL, "🔴 $msg")

        public fun warning(msg: String): Unit = present(LogCategory.GENERAL, "⚠\uFE0F $msg")
    }

    public object Navigation {
        public fun log(msg: String): Unit = present(LogCategory.NAVIGATION, msg)
    }

    public object Network {
        public fun log(msg: String): Unit = present(LogCategory.NETWORK, msg)
    }

    public object Performance {
        public fun navigationStarted(screen: String) {
            PerformanceTracker.navigationStarted(screen)
        }

        public fun navigationFinished(screen: String) {
            PerformanceTracker.navigationFinished(screen) { duration, summary ->
                present(LogCategory.PERFORMANCE, "🧭 [$screen] → ${duration}ms\n$summary")
            }
        }
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
