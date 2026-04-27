package com.grippo.data.features.api.metrics.profile.models

public data class TrainingLoadProfileArtifacts(
    val topExercises: List<TopExerciseContribution>,
    val topMuscles: List<TopMuscleContribution>,
    val totalExercisesCount: Int, // unique exercises that contributed to the period
    val totalMusclesCount: Int,   // unique muscles worked across the period
    val compoundRatio: Int, // % of stimulus from compound exercises (0..100)
    val pushRatio: Int,     // % of stimulus from PUSH force-type lifts (0..100)
    val pullRatio: Int,     // % of stimulus from PULL force-type lifts (0..100)
    val hingeRatio: Int,    // % of stimulus from HINGE force-type lifts (0..100)
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
        )
    }
}