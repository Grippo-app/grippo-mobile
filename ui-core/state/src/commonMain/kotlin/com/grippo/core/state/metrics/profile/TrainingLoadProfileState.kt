package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.muscles.MuscleEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TrainingLoadProfileState(
    val kind: TrainingProfileKindState,
    val dimensions: ImmutableList<TrainingDimensionScoreState>,
    val dominant: TrainingDimensionKindState?,
    val confidence: Int,
    val artifacts: TrainingLoadProfileArtifactsState = TrainingLoadProfileArtifactsState.empty(),
) {
    @Composable
    public fun title(): String = kind.title(this)

    @Composable
    public fun subtitle(): String = kind.subtitle(this)

    @Composable
    public fun tip(): String? = kind.tip(this)

    public fun scoreOf(kind: TrainingDimensionKindState): Int {
        return dimensions.firstOrNull { it.kind == kind }?.score?.coerceIn(0, 100) ?: 0
    }
}

public fun stubTrainingLoadProfile(): TrainingLoadProfileState {
    return TrainingLoadProfileState(
        kind = TrainingProfileKindState.Powerbuilding,
        dimensions = persistentListOf(
            TrainingDimensionScoreState(
                kind = TrainingDimensionKindState.Strength,
                score = 78
            ),
            TrainingDimensionScoreState(
                kind = TrainingDimensionKindState.Hypertrophy,
                score = 68
            ),
            TrainingDimensionScoreState(
                kind = TrainingDimensionKindState.Endurance,
                score = 32
            )
        ),
        dominant = TrainingDimensionKindState.Strength,
        confidence = 74,
        artifacts = stubTrainingLoadProfileArtifacts(),
    )
}

public fun stubTrainingLoadProfileArtifacts(): TrainingLoadProfileArtifactsState {
    return TrainingLoadProfileArtifactsState(
        topExercises = persistentListOf(
            TopExerciseContributionState(
                exampleId = "ex-1",
                name = "Back Squat",
                totalSets = 12,
                stimulusShare = 28,
                heaviestWeight = 140f,
                estimatedOneRepMax = 155f,
                category = CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-2",
                name = "Bench Press",
                totalSets = 10,
                stimulusShare = 22,
                heaviestWeight = 105f,
                estimatedOneRepMax = 118f,
                category = CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-3",
                name = "Deadlift",
                totalSets = 6,
                stimulusShare = 18,
                heaviestWeight = 180f,
                estimatedOneRepMax = 198f,
                category = CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-4",
                name = "Barbell Row",
                totalSets = 8,
                stimulusShare = 12,
                heaviestWeight = 85f,
                estimatedOneRepMax = 96f,
                category = CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-5",
                name = "Bicep Curl",
                totalSets = 9,
                stimulusShare = 8,
                heaviestWeight = 22f,
                estimatedOneRepMax = 26f,
                category = CategoryEnumState.ISOLATION,
            ),
        ),
        topMuscles = persistentListOf(
            TopMuscleContributionState(MuscleEnumState.QUADRICEPS, 26),
            TopMuscleContributionState(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL, 22),
            TopMuscleContributionState(MuscleEnumState.LATISSIMUS_DORSI, 15),
            TopMuscleContributionState(MuscleEnumState.GLUTEAL, 12),
            TopMuscleContributionState(MuscleEnumState.BICEPS, 9),
        ),
        totalExercisesCount = 9,
        totalMusclesCount = 7,
        compoundRatio = 72,
        pushRatio = 46,
        pullRatio = 38,
        hingeRatio = 16,
    )
}
