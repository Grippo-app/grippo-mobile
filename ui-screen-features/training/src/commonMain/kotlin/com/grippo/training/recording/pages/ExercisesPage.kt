package com.grippo.training.recording.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.placeholder.ScreenPlaceholder
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.no_exercises_yet
import com.grippo.training.recording.RecordingTab
import com.grippo.training.recording.TrainingRecordingContract
import com.grippo.training.recording.TrainingRecordingScreen
import com.grippo.training.recording.TrainingRecordingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExercisesPage(
    modifier: Modifier = Modifier,
    state: TrainingRecordingState,
    contract: TrainingRecordingContract
) {
    val exercises = remember(state.exercises) { state.exercises }

    AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        },
        targetState = exercises.isEmpty()
    ) {
        when (it) {
            true -> ScreenPlaceholder(
                modifier = Modifier.fillMaxSize(),
                text = AppTokens.strings.res(Res.string.no_exercises_yet),
            )

            false -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                items(
                    items = exercises,
                    key = { k -> k.id }
                ) { exercise ->
                    val editExerciseProvider = remember(exercise.id) {
                        { contract.onEditExercise(exercise.id) }
                    }

                    val deleteExerciseProvider = remember(exercise.id) {
                        { contract.onDeleteExercise(exercise.id) }
                    }

                    SwipeToReveal(
                        modifier = Modifier.animateItem(),
                        actions = {
                            Button(
                                modifier = Modifier.padding(end = AppTokens.dp.screen.horizontalPadding),
                                content = ButtonContent.Icon(
                                    icon = AppTokens.icons.Cancel
                                ),
                                style = ButtonStyle.Error,
                                onClick = deleteExerciseProvider
                            )
                        }
                    ) {
                        ExerciseCard(
                            modifier = Modifier
                                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                                .fillMaxWidth(),
                            value = exercise,
                            style = ExerciseCardStyle.Medium(
                                onClick = editExerciseProvider
                            ),
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExercisesPagePreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = stubTraining().exercises,
                tab = RecordingTab.Exercises
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ExercisesPageEmptyPreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = persistentListOf(),
                tab = RecordingTab.Exercises
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}