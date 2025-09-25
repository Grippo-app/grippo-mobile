package com.grippo.calculation.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.calculation.muscle.factory.MuscleColorSource
import com.grippo.calculation.muscle.factory.MuscleColorStrategy
import com.grippo.state.muscles.MuscleEnumState

@Immutable
public data class DistributionSlice(
    val id: String,
    val label: String,
    val value: Float,
    val color: Color,
)

@Immutable
public data class DistributionBreakdown(
    val slices: List<DistributionSlice>,
)

@Immutable
public data class MuscleLoadEntry(
    val label: String,
    val value: Float,
    override val color: Color,
    override val muscles: List<MuscleEnumState>,
) : MuscleColorSource

@Immutable
public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

@Immutable
public data class MuscleLoadVisualization(
    val perMuscle: MuscleLoadBreakdown,
    val perGroup: MuscleLoadBreakdown,
)

public fun MuscleLoadBreakdown.toColorStrategy(): MuscleColorStrategy.BySources =
    MuscleColorStrategy.BySources(entries)

public fun MuscleLoadBreakdown.toColorSources(): List<MuscleColorSource> = entries

@Immutable
public data class MetricPoint(
    val label: String,
    val value: Float,
    val color: Color,
)

@Immutable
public data class MetricSeries(
    val points: List<MetricPoint>,
)

@Immutable
public data class MuscleLoadMatrix(
    val rows: Int,
    val cols: Int,
    val values01: List<Float>,
    val rowLabels: List<String> = emptyList(),
    val colLabels: List<String> = emptyList(),
)
