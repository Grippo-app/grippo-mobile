package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.MuscleLoadTimelineState
import com.grippo.data.features.api.metrics.models.MuscleLoadTimeline
import kotlinx.collections.immutable.toPersistentList

public fun MuscleLoadTimeline?.toState(): MuscleLoadTimelineState? {
    val value = this ?: return null
    val rows = value.rows
    val buckets = value.buckets
    if (rows.isEmpty() || buckets.isEmpty()) return null
    val expected = rows.size * buckets.size
    if (value.values01.size != expected) return null

    return MuscleLoadTimelineState(
        rows = rows.size,
        cols = buckets.size,
        values01 = value.values01,
        rowLabels = rows.map { it.label }.toPersistentList(),
        colLabels = buckets.map { it.label }.toPersistentList(),
    )
}
