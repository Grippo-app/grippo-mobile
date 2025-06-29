package com.grippo.presentation.api.muscles.factory

import com.grippo.presentation.api.exercise.example.models.ExerciseExampleBundleState
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

public sealed interface MuscleColorStrategy {
    public data object Monochrome : MuscleColorStrategy

    public data class ByAlpha(
        val bundles: ImmutableList<ExerciseExampleBundleState>,
    ) : MuscleColorStrategy

    public data class BySelection(
        val group: MuscleGroupState<*>,
        val selectedIds: ImmutableSet<String>
    ) : MuscleColorStrategy
}