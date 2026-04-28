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

    /** % of the push/pull/hinge pool from PUSH lifts (0..100). */
    val pushRatio: Int,
    /** % of the push/pull/hinge pool from PULL lifts (0..100). */
    val pullRatio: Int,
    /** % of the push/pull/hinge pool from HINGE lifts (0..100). */
    val hingeRatio: Int,

    /** Stimulus share carried by the single biggest exercise (0..100). */
    val topExerciseShare: Int,
    /** Stimulus share of the top-2 muscles combined (0..100). */
    val topTwoMusclesShare: Int,
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

    public val hasForceTypePool: Boolean
        get() = pushRatio + pullRatio + hingeRatio > 0

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
                topExerciseShare = 0,
                topTwoMusclesShare = 0,
            )
    }
}
