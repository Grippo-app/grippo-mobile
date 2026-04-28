package com.grippo.data.features.api.metrics.profile.models

public data class TrainingLoadProfileArtifacts(
    val topExercises: List<TopExerciseContribution>,
    val topMuscles: List<TopMuscleContribution>,
    val totalExercisesCount: Int, // unique exercises that contributed to the period
    val totalMusclesCount: Int,   // unique muscles worked across the period

    val compoundRatio: Int, // % of stimulus from compound exercises (0..100)

    /** % of the push/pull/hinge pool from PUSH force-type lifts (0..100). */
    val pushRatio: Int,
    /** % of the push/pull/hinge pool from PULL force-type lifts (0..100). */
    val pullRatio: Int,
    /** % of the push/pull/hinge pool from HINGE force-type lifts (0..100). */
    val hingeRatio: Int,

    /** Stimulus share of the single biggest exercise (0..100). */
    val topExerciseShare: Int,
    /** Stimulus share of the top-2 muscles combined (0..100). */
    val topTwoMusclesShare: Int,
) {
    public companion object {
        public fun empty(): TrainingLoadProfileArtifacts = TrainingLoadProfileArtifacts(
            topExercises = emptyList(),
            topMuscles = emptyList(),
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
