package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.muscle.models.MuscleEnum
import kotlinx.datetime.LocalDateTime

public data class MuscleLoadTimeline(
    val rows: List<MuscleLoadTimelineRow>,
    val buckets: List<MuscleLoadTimelineBucket>,
    val values01: List<Float>,
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
)

public enum class MuscleLoadTimelineMetric {
    Volume,
    Repetitions,
}
