package com.grippo.logger

import com.grippo.logger.internal.getCallerLocation

public object AppLogger {

    private var logListener: ((category: LogCategory, message: String) -> Unit)? = null

    public fun setLogListener(listener: (category: LogCategory, message: String) -> Unit) {
        logListener = listener
    }

    public fun debug(msg: String) {
        onDebug {
            val category = LogCategory.GENERAL
            println(category.name + " " + msg)
            logListener?.invoke(category, msg)
        }
    }

    public fun error(msg: String) {
        onDebug {
            val category = LogCategory.GENERAL
            println(category.name + " " + "🔴 $msg")
            logListener?.invoke(category, "🔴 $msg")
        }
    }

    public fun success(msg: String) {
        onDebug {
            val category = LogCategory.GENERAL
            println(category.name + " " + "🟢 $msg")
            logListener?.invoke(category, "🟢 $msg")
        }
    }

    public fun network(msg: String) {
        onDebug {
            val category = LogCategory.NETWORK
            println(category.name + " " + msg)
            logListener?.invoke(category, msg)
        }
    }

    public fun navigation(msg: String) {
        onDebug {
            val category = LogCategory.NAVIGATION
            println(category.name + " " + msg)
            logListener?.invoke(category, msg)
        }
    }

    public fun <T> mapping(value: T?, lazyMessage: () -> String, throwable: Throwable? = null): T? {
        if (value != null) return value

        val category = LogCategory.MAPPING
        val location = getCallerLocation()

        val fullMessage = "🧩 [Mapping] ${lazyMessage()} $location"
        onDebug {
            println(category.name + " " + fullMessage + " " + throwable)
            logListener?.invoke(category, "$fullMessage $throwable")
        }

        return null
    }

    private fun onDebug(action: () -> Unit) {
        action.invoke()
    }
}