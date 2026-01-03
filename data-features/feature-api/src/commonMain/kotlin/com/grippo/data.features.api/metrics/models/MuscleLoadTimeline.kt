package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.muscle.models.MuscleEnum
import kotlinx.datetime.LocalDateTime

public data class MuscleLoadTimeline(
    val rows: List<MuscleLoadTimelineRow>,
    val buckets: List<MuscleLoadTimelineBucket>,
    /**
     * Raw values (same shape as values01).
     */
    val values: List<Float>,
    /**
     * Normalized values (0..1) for heatmap rendering.
     *
     * Important: we avoid percentile-based normalization because it can hide peaks.
     */
    val values01: List<Float>,
    val normalization: MuscleLoadTimelineNormalization,
)

public data class MuscleLoadTimelineRow(
    val id: String,
    val label: String,
    val muscles: List<MuscleEnum>,
)

public data class MuscleLoadTimelineBucket(
    val label: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val sessionsCount: Int,
)

public enum class MuscleLoadTimelineMetric {
    Volume,
    Repetitions,
    Sets,
}

public enum class MuscleLoadTimelineNormalization {
    Absolute,
    PerRow,
    PerColumn,
}
