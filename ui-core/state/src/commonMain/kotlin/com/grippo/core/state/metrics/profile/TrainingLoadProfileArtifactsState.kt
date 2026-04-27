package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TrainingLoadProfileArtifactsState(
    val topExercises: ImmutableList<TopExerciseContributionState>,
    val topMuscles: ImmutableList<TopMuscleContributionState>,
    val totalExercisesCount: Int,
    val totalMusclesCount: Int,
    val compoundRatio: Int,
    val pushRatio: Int,
    val pullRatio: Int,
    val hingeRatio: Int,
) {
    public val isEmpty: Boolean
        get() = topExercises.isEmpty() &&
                topMuscles.isEmpty() &&
                compoundRatio == 0 &&
                pushRatio == 0 &&
                pullRatio == 0 &&
                hingeRatio == 0

    public val isolationRatio: Int
        get() = (100 - compoundRatio).coerceIn(0, 100)

    public companion object {
        public fun empty(): TrainingLoadProfileArtifactsState =
            TrainingLoadProfileArtifactsState(
                topExercises = persistentListOf(),
                topMuscles = persistentListOf(),
                totalExercisesCount = 0,
                totalMusclesCount = 0,
                compoundRatio = 0,
                pushRatio = 0,
                pullRatio = 0,
                hingeRatio = 0,
            )
    }
}
