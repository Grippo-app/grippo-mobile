package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.PerformanceSession
import platform.Foundation.NSDate
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970

internal actual object PerformanceTracker {

    private val sessionStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0
    private var peakThreadCount = 0

    actual fun navigationStarted(session: PerformanceSession) {
        sessionStarts[session.token] = currentTimeMillis()
    }

    actual fun navigationFinished(
        session: PerformanceSession,
        onLogged: (durationMs: Long, summary: String) -> Unit,
    ) {
        val start = sessionStarts.remove(session.token) ?: return
        val duration = currentTimeMillis() - start
        record(duration)
        val summary = buildSummary()
        onLogged(duration, summary)
    }

    actual fun navigationCancelled(session: PerformanceSession) {
        sessionStarts.remove(session.token)
    }

    private fun record(duration: Long) {
        totalRenderTimeMs += duration
        renderCount++
        peakThreadCount = maxOf(
            peakThreadCount,
            NSThread.callStackReturnAddresses.count()
        )
    }

    private fun buildSummary(): String {
        if (renderCount == 0) return ""
        val avg = totalRenderTimeMs / renderCount
        val icon = performanceIcon(avg)
        return "📱 $renderCount screens · $icon ${avg}ms avg · 🧠 $peakThreadCount threads"
    }

    private fun currentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    private fun performanceIcon(ms: Long): String = when {
        ms <= 130 -> "🟢"
        ms <= 250 -> "🟡"
        else -> "🔴"
    }
}
