package com.grippo.training.recording.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonColorTokens
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.Gym
import com.grippo.design.resources.provider.no_exercises_yet
import com.grippo.state.trainings.stubTraining
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

    if (exercises.isEmpty()) {
        Placeholder(
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(top = AppTokens.dp.contentPadding.content),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            items(
                items = exercises,
                key = { it.id }
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
                            style = ButtonStyle.Custom(
                                enabled = ButtonColorTokens(
                                    background = AppTokens.colors.semantic.error,
                                    icon = AppTokens.colors.button.iconPrimary,
                                    content = AppTokens.colors.button.textPrimary,
                                    border = AppTokens.colors.semantic.error,
                                ),
                                disabled = ButtonColorTokens(
                                    background = AppTokens.colors.button.backgroundPrimaryDisabled,
                                    content = AppTokens.colors.button.contentPrimaryDisabled,
                                    border = Color.Transparent,
                                    icon = AppTokens.colors.button.contentPrimaryDisabled
                                ),
                            ),
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

@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .alpha(0.2f)
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Icon(
            modifier = Modifier
                .background(AppTokens.colors.background.card, CircleShape)
                .size(200.dp)
                .padding(24.dp),
            imageVector = AppTokens.icons.Gym,
            contentDescription = null,
            tint = AppTokens.colors.icon.primary
        )

        Text(
            text = AppTokens.strings.res(Res.string.no_exercises_yet),
            textAlign = TextAlign.Center,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun ExercisesPagePreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
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
                exercises = persistentListOf(),
                tab = RecordingTab.Exercises
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}