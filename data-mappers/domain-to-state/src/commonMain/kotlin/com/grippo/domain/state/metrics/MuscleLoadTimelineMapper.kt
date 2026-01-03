package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.MuscleLoadTimelineNormalizationState
import com.grippo.core.state.metrics.MuscleLoadTimelineState
import com.grippo.data.features.api.metrics.models.MuscleLoadTimeline
import com.grippo.data.features.api.metrics.models.MuscleLoadTimelineNormalization
import kotlinx.collections.immutable.toPersistentList

public fun MuscleLoadTimeline?.toState(): MuscleLoadTimelineState? {
    val value = this ?: return null
    val rows = value.rows
    val buckets = value.buckets
    if (rows.isEmpty() || buckets.isEmpty()) return null
    val expected = rows.size * buckets.size
    if (value.values.size != expected) return null
    if (value.values01.size != expected) return null

    return MuscleLoadTimelineState(
        rows = rows.size,
        cols = buckets.size,
        values = value.values,
        values01 = value.values01,
        normalization = value.normalization.toState(),
        rowLabels = rows.map { it.label }.toPersistentList(),
        colLabels = buckets.map { it.label }.toPersistentList(),
        colSessionsCount = buckets.map { it.sessionsCount }.toPersistentList(),
    )
}

private fun MuscleLoadTimelineNormalization.toState(): MuscleLoadTimelineNormalizationState {
    return when (this) {
        MuscleLoadTimelineNormalization.Absolute -> MuscleLoadTimelineNormalizationState.Absolute
        MuscleLoadTimelineNormalization.PerRow -> MuscleLoadTimelineNormalizationState.PerRow
        MuscleLoadTimelineNormalization.PerColumn -> MuscleLoadTimelineNormalizationState.PerColumn
    }
}
