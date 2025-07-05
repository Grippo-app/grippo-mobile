package com.grippo.logger

import com.grippo.logger.internal.PerformanceTracker
import com.grippo.logger.internal.getCallerLocation

public object AppLogger {

    private var logListener: ((category: LogCategory, message: String) -> Unit)? = null

    public fun setLogListener(listener: (category: LogCategory, message: String) -> Unit) {
        logListener = listener
    }

    public fun error(msg: String) {
        present(LogCategory.GENERAL, "🔴 $msg")
    }

    public fun warning(msg: String) {
        present(LogCategory.GENERAL, "⚠\uFE0F $msg")
    }

    public fun network(msg: String) {
        present(LogCategory.NETWORK, msg)
    }

    public fun navigation(msg: String) {
        present(LogCategory.NAVIGATION, msg)
    }

    public fun performance(screen: String) {
        val duration = PerformanceTracker.markScreen(screen)
        if (duration != null) {
            val summary = PerformanceTracker.logSummary()
            present(LogCategory.PERFORMANCE, "🧭 [$screen] → ${duration}ms\n$summary")
        }
    }

    public fun <T> checkOrLog(value: T?, msg: () -> String): T? {
        if (value != null) return value
        val location = getCallerLocation()
        present(LogCategory.MAPPING, "${msg()} $location")
        return null
    }

    private fun present(category: LogCategory, msg: String) {
        onDebug {
            println(msg)
            logListener?.invoke(category, msg)
        }
    }

    private fun onDebug(action: () -> Unit) {
        action.invoke()
    }
}