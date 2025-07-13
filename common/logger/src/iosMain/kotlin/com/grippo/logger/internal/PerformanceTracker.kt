package com.grippo.logger.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970

internal actual object PerformanceTracker {

    private val screenStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0
    private var peakThreadCount = 0

    actual fun navigate(screen: String, onLogged: (durationMs: Long, summary: String) -> Unit) {
        val now = currentTimeMillis()
        val start = screenStarts.remove(screen)
        if (start != null) {
            val duration = now - start
            record(duration)
            val summary = buildSummary()
            onLogged(duration, summary)
        } else {
            screenStarts[screen] = now
        }
    }

    @Composable
    actual fun Track(screen: String, onOpened: () -> Unit) {
        val tracked = remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    if (!tracked.value) {
                        tracked.value = true
                        onOpened()
                    }
                }
        )
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
        return (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    private fun performanceIcon(ms: Long): String = when {
        ms <= 130 -> "🟢"
        ms <= 250 -> "🟡"
        else -> "🔴"
    }
}