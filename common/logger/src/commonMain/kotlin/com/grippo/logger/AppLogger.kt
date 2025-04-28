package com.grippo.logger

public object AppLogger {

    private var logListener: ((category: LogCategory, message: String) -> Unit)? = null

    public fun setLogListener(listener: (category: LogCategory, message: String) -> Unit) {
        logListener = listener
    }

    public fun debug(msg: String) {
        onDebug {
            val category = LogCategory.GENERAL
//            Log.d(category.name, msg)
            logListener?.invoke(category, msg)
        }
    }

    public fun error(msg: String) {
        onDebug {
            val category = LogCategory.GENERAL
//            Log.e(category.name, "🔴 $msg")
            logListener?.invoke(category, "🔴 $msg")
        }
    }

    public fun success(msg: String) {
        onDebug {
            val category = LogCategory.GENERAL
//            Log.d(category.name, "🟢 $msg")
            logListener?.invoke(category, "🟢 $msg")
        }
    }

    public fun network(msg: String) {
        onDebug {
            val category = LogCategory.NETWORK
//            Log.d(category.name, msg)
            logListener?.invoke(category, msg)
        }
    }

    private fun onDebug(action: () -> Unit) {
        action.invoke()
    }
}