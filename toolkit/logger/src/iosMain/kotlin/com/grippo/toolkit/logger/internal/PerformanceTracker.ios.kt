package com.grippo.toolkit.logger.internal

import platform.Foundation.NSDate
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970

internal actual object PerformanceTracker {

    private val screenStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0
    private var peakThreadCount = 0

    actual fun navigationStarted(screen: String) {
        screenStarts[screen] = currentTimeMillis()
    }

    actual fun navigationFinished(
        screen: String,
        onLogged: (durationMs: Long, summary: String) -> Unit,
    ) {
        val start = screenStarts.remove(screen) ?: return
        val duration = currentTimeMillis() - start
        record(duration)
        val summary = buildSummary()
        onLogged(duration, summary)
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
