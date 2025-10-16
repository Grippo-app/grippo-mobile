package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.PerformanceSession

internal expect object PerformanceTracker {
    fun navigationStarted(session: PerformanceSession)
    fun navigationFinished(
        session: PerformanceSession,
        onLogged: (durationMs: Long, summary: String) -> Unit,
    )

    fun navigationCancelled(session: PerformanceSession)
}
