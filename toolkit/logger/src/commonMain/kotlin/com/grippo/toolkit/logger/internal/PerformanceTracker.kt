package com.grippo.toolkit.logger.internal

internal expect object PerformanceTracker {
    fun navigationStarted(screen: String)
    fun navigationFinished(screen: String, onLogged: (durationMs: Long, summary: String) -> Unit)
}
