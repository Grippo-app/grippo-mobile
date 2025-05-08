package com.grippo.logger

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

    private fun onDebug(action: () -> Unit) {
        action.invoke()
    }
}