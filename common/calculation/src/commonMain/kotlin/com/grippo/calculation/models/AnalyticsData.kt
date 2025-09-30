package com.grippo.calculation.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

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
public data class MuscleImages(
    val front: ImageVector,
    val back: ImageVector,
)

@Immutable
public data class MuscleLoadSummary(
    val perGroup: MuscleLoadBreakdown,
    val images: MuscleImages?,
)

public sealed interface Metric {
    public data object TONNAGE : Metric
    public data object REPS : Metric
}

@Immutable
public sealed interface MuscleColorStrategy {

    @Immutable
    public data class ByScaleStops(
        val bundles: ImmutableList<ExerciseExampleBundleState>,
    ) : MuscleColorStrategy

    @Immutable
    public data class BySelection(
        val group: MuscleGroupState<*>,
        val selectedIds: ImmutableSet<String>,
    ) : MuscleColorStrategy

    @Immutable
    public data class BySources(
        val sources: List<MuscleColorSource>,
    ) : MuscleColorStrategy
}

@Immutable
public interface MuscleColorSource {
    public val muscles: List<MuscleEnumState>
    public val color: Color
}

@Immutable
public sealed interface DistributionWeighting {
    public data object Count : DistributionWeighting
    public data object Sets : DistributionWeighting
    public data object Reps : DistributionWeighting
    public data object Volume : DistributionWeighting
}

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
