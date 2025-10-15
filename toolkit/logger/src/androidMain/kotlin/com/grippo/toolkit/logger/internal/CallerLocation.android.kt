package com.grippo.toolkit.logger.internal

internal actual fun getCallerLocation(depth: Int): String {
    val caller = Throwable().stackTrace.getOrNull(depth)
    return if (caller != null) {
        "(${caller.fileName}:${caller.lineNumber})"
    } else {
        "(unknown)"
    }
}
