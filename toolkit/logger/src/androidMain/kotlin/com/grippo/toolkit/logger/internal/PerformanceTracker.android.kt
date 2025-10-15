package com.grippo.toolkit.logger.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

internal actual object PerformanceTracker {

    private val screenStarts = mutableMapOf<String, Long>()
    private var totalRenderTimeMs = 0L
    private var renderCount = 0

    actual fun navigate(screen: String, onLogged: (durationMs: Long, summary: String) -> Unit) {
        val now = System.currentTimeMillis()
        val start = screenStarts.remove(screen)

        if (start != null) {
            val duration = now - start
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
}
