package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class ExerciseDistributionState<T>(
    val entries: ImmutableList<ExerciseDistributionEntryState<T>>,
)

@Immutable
public data class ExerciseDistributionEntryState<T>(
    val key: T,
    val value: Float,
)

public fun stubCategoryDistributionState(): ExerciseDistributionState<CategoryEnumState> {
    val entries = listOf(
        ExerciseDistributionEntryState(
            key = CategoryEnumState.COMPOUND,
            value = 40f
        ),
        ExerciseDistributionEntryState(
            key = CategoryEnumState.ISOLATION,
            value = 60f
        ),
    ).toPersistentList()
    return ExerciseDistributionState(entries = entries)
}

public fun stubWeightDistributionState(): ExerciseDistributionState<WeightTypeEnumState> {
    val entries = listOf(
        ExerciseDistributionEntryState(
            key = WeightTypeEnumState.FREE,
            value = 55f
        ),
        ExerciseDistributionEntryState(
            key = WeightTypeEnumState.FIXED,
            value = 30f
        ),
        ExerciseDistributionEntryState(
            key = WeightTypeEnumState.BODY_WEIGHT,
            value = 15f
        ),
    ).toPersistentList()
    return ExerciseDistributionState(entries = entries)
}

public fun stubForceDistributionState(): ExerciseDistributionState<ForceTypeEnumState> {
    val entries = listOf(
        ExerciseDistributionEntryState(
            key = ForceTypeEnumState.PUSH,
            value = 45f
        ),
        ExerciseDistributionEntryState(
            key = ForceTypeEnumState.PULL,
            value = 35f
        ),
        ExerciseDistributionEntryState(
            key = ForceTypeEnumState.HINGE,
            value = 20f
        ),
    ).toPersistentList()
    return ExerciseDistributionState(entries = entries)
}
