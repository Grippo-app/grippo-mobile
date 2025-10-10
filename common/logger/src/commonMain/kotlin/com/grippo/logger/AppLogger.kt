package com.grippo.logger

import androidx.compose.runtime.Composable
import com.grippo.logger.platform.PerformanceTracker
import com.grippo.logger.platform.getCallerLocation

public object AppLogger {

    private var logListener: ((LogCategory, String) -> Unit)? = null

    public fun setLogListener(listener: (LogCategory, String) -> Unit) {
        logListener = listener
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
        public fun navigate(screen: String) {
            PerformanceTracker.navigate(screen) { duration, summary ->
                present(LogCategory.PERFORMANCE, "🧭 [$screen] → ${duration}ms\n$summary")
            }
        }

        @Composable
        public fun Open(screen: String) {
            PerformanceTracker.Track(screen) {
                PerformanceTracker.navigate(screen) { duration, summary ->
                    present(LogCategory.PERFORMANCE, "🧭 [$screen] → ${duration}ms\n$summary")
                }
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
            logListener?.invoke(category, msg)
        }
    }

    public object AI {
        public fun prompt(msg: String): Unit = present(LogCategory.PROMPT, msg)
        public fun answer(msg: String): Unit = present(LogCategory.ANSWER, msg)
    }

    private fun onDebug(action: () -> Unit) {
        action.invoke()
    }
}