package com.grippo.data.features.api.metrics.models

public data class TrainingLoadProfile(
    val kind: TrainingProfileKind,
    val dimensions: List<TrainingDimensionScore>,
    val dominant: TrainingDimensionKind?,
    val confidence: Int,
    val artifacts: TrainingLoadProfileArtifacts = TrainingLoadProfileArtifacts.empty(),
)

public enum class TrainingDimensionKind {
    Strength,
    Hypertrophy,
    Endurance,
}

public enum class TrainingProfileKind {
    Strength,
    Hypertrophy,
    Endurance,
    Powerbuilding,
    Mixed,
    Easy,
}

public data class TrainingDimensionScore(
    val kind: TrainingDimensionKind,
    val score: Int, // 0..100
)

/**
 * Intermediate calculation artifacts the profile classifier produces on its
 * way to the final [TrainingLoadProfile.kind]. Surfaced so the user can see
 * the *evidence* behind the verdict: which exercises drove the profile, which
 * muscles got the most work, and a few movement-style ratios.
 *
 * All percent fields are 0..100. `topExercises` and `topMuscles` are sorted
 * by their share descending. `totalExercisesCount` / `totalMusclesCount`
 * back the "Top N of M" footer rendered by the detail cards — nothing more.
 */
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
