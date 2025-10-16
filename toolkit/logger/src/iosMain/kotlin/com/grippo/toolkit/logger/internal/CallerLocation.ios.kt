package com.grippo.toolkit.logger.internal

import platform.Foundation.NSThread

internal actual fun getCallerLocation(depth: Int): String {
    val symbols = NSThread.callStackSymbols
    val index = depth + 1
    val frame = symbols.getOrNull(index) ?: return "(native)"
    return frame.toString()
}
