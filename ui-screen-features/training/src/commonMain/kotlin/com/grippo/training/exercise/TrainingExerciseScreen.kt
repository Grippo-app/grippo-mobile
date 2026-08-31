package com.grippo.training.exercise

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingExerciseScreen(
    state: TrainingExerciseState,
    loaders: ImmutableSet<TrainingExerciseLoader>,
    contract: TrainingExerciseContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun TrainingExerciseScreenPreview() {
    PreviewContainer {
        TrainingExerciseScreen(
            state = TrainingExerciseState(
                exercise = stubExercise(),
                exerciseExample = stubExerciseExample(),
                volumeArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
                repetitionArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
            ),
            loaders = persistentSetOf(),
            contract = TrainingExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseScreenEmptyPreview() {
    PreviewContainer {
        TrainingExerciseScreen(
            state = TrainingExerciseState(
                exercise = stubExercise().copy(iterations = persistentListOf()),
                exerciseExample = stubExerciseExample(),
                volumeArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
                repetitionArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
            ),
            loaders = persistentSetOf(),
            contract = TrainingExerciseContract.Empty,
        )
    }
}
