package com.grippo.core.state.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_dimension_endurance
import com.grippo.design.resources.provider.training_dimension_hypertrophy
import com.grippo.design.resources.provider.training_dimension_strength
import com.grippo.design.resources.provider.training_profile_axis_endurance_id
import com.grippo.design.resources.provider.training_profile_axis_hypertrophy_id
import com.grippo.design.resources.provider.training_profile_axis_strength_id
import com.grippo.design.resources.provider.training_profile_kind_easy
import com.grippo.design.resources.provider.training_profile_kind_endurance
import com.grippo.design.resources.provider.training_profile_kind_hypertrophy
import com.grippo.design.resources.provider.training_profile_kind_mixed
import com.grippo.design.resources.provider.training_profile_kind_powerbuilding
import com.grippo.design.resources.provider.training_profile_kind_strength
import com.grippo.design.resources.provider.training_profile_subtitle_easy
import com.grippo.design.resources.provider.training_profile_subtitle_endurance
import com.grippo.design.resources.provider.training_profile_subtitle_hypertrophy
import com.grippo.design.resources.provider.training_profile_subtitle_mixed
import com.grippo.design.resources.provider.training_profile_subtitle_powerbuilding
import com.grippo.design.resources.provider.training_profile_subtitle_strength
import com.grippo.design.resources.provider.training_profile_tip_easy
import com.grippo.design.resources.provider.training_profile_tip_endurance_high
import com.grippo.design.resources.provider.training_profile_tip_endurance_low
import com.grippo.design.resources.provider.training_profile_tip_endurance_medium
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_high
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_low
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_medium
import com.grippo.design.resources.provider.training_profile_tip_mixed
import com.grippo.design.resources.provider.training_profile_tip_powerbuilding
import com.grippo.design.resources.provider.training_profile_tip_strength_high
import com.grippo.design.resources.provider.training_profile_tip_strength_low
import com.grippo.design.resources.provider.training_profile_tip_strength_medium
import com.grippo.design.resources.provider.training_profile_title_easy_session
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TrainingLoadProfileState(
    val kind: TrainingProfileKindState,
    val dimensions: List<TrainingDimensionScoreState>,
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

@Immutable
public enum class TrainingDimensionKindState {
    Strength,
    Hypertrophy,
    Endurance;

    @Composable
    public fun axisId(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_profile_axis_strength_id)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_axis_hypertrophy_id)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_axis_endurance_id)
        }
    }

    @Composable
    public fun label(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_dimension_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_dimension_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_dimension_endurance)
        }
    }
}

@Immutable
public enum class TrainingProfileKindState {
    Strength,
    Hypertrophy,
    Endurance,
    Powerbuilding,
    Mixed,
    Easy;

    @Composable
    public fun label(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_profile_kind_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_kind_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_kind_endurance)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_kind_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_kind_mixed)
            Easy -> AppTokens.strings.res(Res.string.training_profile_kind_easy)
        }
    }

    @Composable
    public fun title(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_title_easy_session)
            Powerbuilding -> label()
            Mixed -> label()
            Strength -> label()
            Hypertrophy -> label()
            Endurance -> label()
        }
    }

    @Composable
    public fun subtitle(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_subtitle_easy)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_subtitle_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_subtitle_mixed)
            Strength -> AppTokens.strings.res(Res.string.training_profile_subtitle_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_subtitle_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_subtitle_endurance)
        }
    }

    @Composable
    public fun tip(profile: TrainingLoadProfileState): String? {
        val c = profile.confidence.coerceIn(0, 100)
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_tip_easy)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_tip_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_tip_mixed)
            Strength -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_strength_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_strength_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_strength_low)
            }

            Hypertrophy -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_low)
            }

            Endurance -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_low)
            }
        }
    }
}

@Immutable
public data class TrainingDimensionScoreState(
    val kind: TrainingDimensionKindState,
    val score: Int, // 0..100
)

/**
 * State-layer mirror of the domain `TrainingLoadProfileArtifacts`. Carries
 * the evidence the classifier used — top exercises, muscle focus, and a few
 * movement-style ratios — so the UI can explain *why* the profile came out
 * the way it did.
 *
 * All percent fields are 0..100. `topExercises` and `topMuscles` are sorted
 * by share descending.
 */
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

public fun stubTrainingLoadProfile(): TrainingLoadProfileState {
    return TrainingLoadProfileState(
        kind = TrainingProfileKindState.Powerbuilding,
        dimensions = listOf(
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
                category = com.grippo.core.state.examples.CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-2",
                name = "Bench Press",
                totalSets = 10,
                stimulusShare = 22,
                heaviestWeight = 105f,
                estimatedOneRepMax = 118f,
                category = com.grippo.core.state.examples.CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-3",
                name = "Deadlift",
                totalSets = 6,
                stimulusShare = 18,
                heaviestWeight = 180f,
                estimatedOneRepMax = 198f,
                category = com.grippo.core.state.examples.CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-4",
                name = "Barbell Row",
                totalSets = 8,
                stimulusShare = 12,
                heaviestWeight = 85f,
                estimatedOneRepMax = 96f,
                category = com.grippo.core.state.examples.CategoryEnumState.COMPOUND,
            ),
            TopExerciseContributionState(
                exampleId = "ex-5",
                name = "Bicep Curl",
                totalSets = 9,
                stimulusShare = 8,
                heaviestWeight = 22f,
                estimatedOneRepMax = 26f,
                category = com.grippo.core.state.examples.CategoryEnumState.ISOLATION,
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
