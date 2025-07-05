package com.grippo.logger.internal

import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970

internal actual object PerformanceTracker {
    private val screenStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0
    private var peakThreadCount = 0

    actual fun markScreen(screenName: String, screenParams: Any?): Long? {
        val key = screenKey(screenName, screenParams)
        val now = currentTimeMillis()

        val start = screenStarts.remove(key)
        return if (start != null) {
            val duration = now - start
            record(screenName, duration)
            duration
        } else {
            screenStarts[key] = now
            null
        }
    }

    private fun record(screenName: String, duration: Long) {
        totalRenderTimeMs += duration
        renderCount++
        peakThreadCount = maxOf(
            peakThreadCount,
            NSThread.callStackReturnAddresses.count()
        )
    }

    actual fun logSummary(): String {
        val avg = if (renderCount == 0) 0 else totalRenderTimeMs / renderCount
        val icon = performanceIcon(avg)
        return "📱 $renderCount screens · $icon ${avg}ms avg · 🧠 $peakThreadCount threads"
    }

    private fun screenKey(name: String, params: Any?) =
        if (params != null) "$name|$params" else name

    private fun currentTimeMillis(): Long {
        return (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    private fun performanceIcon(ms: Long): String = when {
        ms <= 130 -> "🟢"
        ms <= 250 -> "🟡"
        else -> "🔴"
    }
}