package com.grippo.toolkit.logger.internal.platform

import androidx.compose.runtime.Composable

internal expect object PerformanceTracker {
    fun navigate(screen: String, onLogged: (durationMs: Long, summary: String) -> Unit)

    @Composable
    fun Track(screen: String, onOpened: () -> Unit)
}