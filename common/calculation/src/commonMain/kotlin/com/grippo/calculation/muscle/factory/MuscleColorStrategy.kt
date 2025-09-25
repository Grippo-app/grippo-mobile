package com.grippo.calculation.muscle.factory

import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.muscles.MuscleGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

public sealed interface MuscleColorStrategy {

    public data class ByScaleStops(
        val bundles: ImmutableList<ExerciseExampleBundleState>,
    ) : MuscleColorStrategy

    public data class BySelection(
        val group: MuscleGroupState<*>,
        val selectedIds: ImmutableSet<String>,
    ) : MuscleColorStrategy

    public data class BySources(
        val sources: List<MuscleColorSource>,
    ) : MuscleColorStrategy
}
