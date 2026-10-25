package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Immutable
import com.grippo.core.state.achievements.AchievementState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExampleValueState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TrainingRecordState(
    val exerciseExample: ExerciseExampleValueState,
    val achievements: ImmutableList<AchievementState>,
)

public fun stubTrainingRecords(): ImmutableList<TrainingRecordState> {
    val legPress = stubExerciseExampleValueState().copy(
        id = "preview-example-leg-press",
        name = "45-Degree Leg Press",
    )
    val benchPress = stubExerciseExampleValueState().copy(id = "preview-example-bench-press")
    val pullUp = stubExerciseExampleValueState().copy(
        id = "preview-example-pull-up",
        name = "Pull-up",
    )

    return persistentListOf(
        TrainingRecordState(
            exerciseExample = legPress,
            achievements = persistentListOf(
                AchievementState.BestWeight(
                    exerciseExampleId = legPress.id,
                    exerciseId = "preview-exercise-1",
                    iterationId = "preview-iteration-1",
                    weight = VolumeFormatState.of(22.5f),
                ),
                AchievementState.BestTonnage(
                    exerciseExampleId = legPress.id,
                    exerciseId = "preview-exercise-1",
                    tonnage = VolumeFormatState.ofTotal(380f),
                ),
                AchievementState.MaxRepetitions(
                    exerciseExampleId = legPress.id,
                    exerciseId = "preview-exercise-1",
                    iterationId = "preview-iteration-2",
                    repetitions = RepetitionsFormatState.of(10),
                ),
                AchievementState.PeakIntensity(
                    exerciseExampleId = legPress.id,
                    exerciseId = "preview-exercise-1",
                    intensity = IntensityFormatState.of(21.1f),
                ),
            ),
        ),
        TrainingRecordState(
            exerciseExample = benchPress,
            achievements = persistentListOf(
                AchievementState.BestWeight(
                    exerciseExampleId = benchPress.id,
                    exerciseId = "preview-exercise-2",
                    iterationId = "preview-iteration-3",
                    weight = VolumeFormatState.of(92.5f),
                ),
                AchievementState.BestTonnage(
                    exerciseExampleId = benchPress.id,
                    exerciseId = "preview-exercise-2",
                    tonnage = VolumeFormatState.ofTotal(4_200f),
                ),
            ),
        ),
        TrainingRecordState(
            exerciseExample = pullUp,
            achievements = persistentListOf(
                AchievementState.MaxRepetitions(
                    exerciseExampleId = pullUp.id,
                    exerciseId = "preview-exercise-3",
                    iterationId = "preview-iteration-4",
                    repetitions = RepetitionsFormatState.of(14),
                ),
            ),
        ),
    )
}
