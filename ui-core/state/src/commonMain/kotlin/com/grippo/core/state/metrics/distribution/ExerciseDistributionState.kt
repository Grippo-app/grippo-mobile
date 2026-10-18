package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class ExerciseDistributionState<T>(
    val weighting: DistributionWeightingState,
    val entries: ImmutableList<ExerciseDistributionEntryState<T>>,
    /** Completed sets omitted from the positive-volume total; zero for other weightings. */
    val excludedVolumeSetCount: Long,
)

public fun stubCategoryDistribution(): ExerciseDistributionState<CategoryEnumState> {
    val entries = listOf(
        ExerciseDistributionEntryState(
            key = CategoryEnumState.COMPOUND,
            value = 4.0
        ),
        ExerciseDistributionEntryState(
            key = CategoryEnumState.ISOLATION,
            value = 6.0
        ),
    ).toPersistentList()
    return ExerciseDistributionState(
        weighting = DistributionWeightingState.Count,
        entries = entries,
        excludedVolumeSetCount = 0L,
    )
}

public fun stubWeightDistribution(): ExerciseDistributionState<WeightTypeEnumState> {
    val entries = listOf(
        ExerciseDistributionEntryState(
            key = WeightTypeEnumState.FREE,
            value = 11.0
        ),
        ExerciseDistributionEntryState(
            key = WeightTypeEnumState.FIXED,
            value = 6.0
        ),
        ExerciseDistributionEntryState(
            key = WeightTypeEnumState.BODY_WEIGHT,
            value = 3.0
        ),
    ).toPersistentList()
    return ExerciseDistributionState(
        weighting = DistributionWeightingState.Count,
        entries = entries,
        excludedVolumeSetCount = 0L,
    )
}

public fun stubForceDistribution(): ExerciseDistributionState<ForceTypeEnumState> {
    val entries = listOf(
        ExerciseDistributionEntryState(
            key = ForceTypeEnumState.PUSH,
            value = 9.0
        ),
        ExerciseDistributionEntryState(
            key = ForceTypeEnumState.PULL,
            value = 7.0
        ),
        ExerciseDistributionEntryState(
            key = ForceTypeEnumState.HINGE,
            value = 4.0
        ),
    ).toPersistentList()
    return ExerciseDistributionState(
        weighting = DistributionWeightingState.Count,
        entries = entries,
        excludedVolumeSetCount = 0L,
    )
}
