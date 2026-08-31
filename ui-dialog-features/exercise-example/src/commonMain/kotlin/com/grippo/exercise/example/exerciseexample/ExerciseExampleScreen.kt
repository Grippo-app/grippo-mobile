package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.achievements.stubAchievements
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.metrics.performance.stubEstimatedOneRepMax
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview1() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                example = stubExerciseExample(),
                recent = stubExercises()
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf(),
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview2() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                example = stubExerciseExample(),
                recent = stubExercises(),
                estimatedOneRepMax = stubEstimatedOneRepMax(),
                achievements = stubAchievements()
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewWithAction() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                mode = ExerciseExampleModeState.Action(title = "Change"),
                example = stubExerciseExample(),
                recent = stubExercises(),
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
