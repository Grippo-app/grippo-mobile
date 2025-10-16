package com.grippo.toolkit.logger.internal

internal actual object PerformanceTracker {

    private val screenStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0

    actual fun navigationStarted(screen: String) {
        screenStarts[screen] = System.currentTimeMillis()
    }

    actual fun navigationFinished(
        screen: String,
        onLogged: (durationMs: Long, summary: String) -> Unit,
    ) {
        val start = screenStarts.remove(screen) ?: return
        val duration = System.currentTimeMillis() - start
        record(duration, onLogged)
    }

    private fun record(
        duration: Long,
        onLogged: (durationMs: Long, summary: String) -> Unit,
    ) {
        totalRenderTimeMs += duration
        renderCount++

        val avg = totalRenderTimeMs / renderCount
        val icon = when {
            avg <= 130 -> "🟢"
            avg <= 250 -> "🟡"
            else -> "🔴"
        }

        val summary = "📱 $renderCount screens · $icon ${avg}ms avg"
        onLogged(duration, summary)
    }
}
