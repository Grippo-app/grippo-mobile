package com.grippo.logger.internal

internal actual object PerformanceTracker {
    private val screenStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0
    private var peakThreadCount = 0
    private var peakUsedHeapKb = 0L
    private var minFreeHeapKb = Long.MAX_VALUE

    actual fun markScreen(screenName: String, screenParams: Any?): Long? {
        val key = screenKey(screenName, screenParams)
        val now = System.currentTimeMillis()

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

        val rt = Runtime.getRuntime()
        val usedHeapKb = (rt.totalMemory() - rt.freeMemory()) / 1024
        val freeHeapKb = rt.freeMemory() / 1024

        peakUsedHeapKb = maxOf(peakUsedHeapKb, usedHeapKb)
        minFreeHeapKb = minOf(minFreeHeapKb, freeHeapKb)
        peakThreadCount = maxOf(peakThreadCount, Thread.activeCount())
    }

    actual fun logSummary(): String {
        val avg = if (renderCount == 0) 0 else totalRenderTimeMs / renderCount
        val icon = performanceIcon(avg)
        return "📱 $renderCount screens · $icon ${avg}ms avg · 🧠 $peakThreadCount threads · 💾 ${peakUsedHeapKb}KB heap · 🧹 minFree: ${minFreeHeapKb}KB"
    }

    private fun screenKey(name: String, params: Any?) =
        if (params != null) "$name|$params" else name

    private fun performanceIcon(ms: Long): String = when {
        ms <= 130 -> "🟢"
        ms <= 250 -> "🟡"
        else -> "🔴"
    }
}