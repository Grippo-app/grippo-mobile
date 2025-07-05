package com.grippo.logger.internal

internal expect object PerformanceTracker {
    /**
     * @return renderDurationMs if second call (screen completed), or null if it's the first call (start).
     */
    fun markScreen(screenName: String, screenParams: Any? = null): Long?
    fun logSummary(): String
}
