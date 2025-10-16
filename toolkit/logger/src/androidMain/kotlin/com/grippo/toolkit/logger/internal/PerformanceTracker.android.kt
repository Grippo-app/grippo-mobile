package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.PerformanceSession

internal actual object PerformanceTracker {

    private val sessionStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0

    actual fun navigationStarted(session: PerformanceSession) {
        sessionStarts[session.token] = System.currentTimeMillis()
    }

    actual fun navigationFinished(
        session: PerformanceSession,
        onLogged: (durationMs: Long, summary: String) -> Unit,
    ) {
        val start = sessionStarts.remove(session.token) ?: return
        val duration = System.currentTimeMillis() - start
        record(duration, onLogged)
    }

    actual fun navigationCancelled(session: PerformanceSession) {
        sessionStarts.remove(session.token)
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
