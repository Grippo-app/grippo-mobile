package com.grippo.training.recording.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonColorTokens
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_exercise_btn
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.training.recording.TrainingRecordingContract
import com.grippo.training.recording.TrainingRecordingState

@Composable
internal fun ExercisesPage(
    modifier: Modifier = Modifier,
    state: TrainingRecordingState,
    contract: TrainingRecordingContract
) {
    val exercises = remember(state.exercises) { state.exercises }

    Column(modifier = modifier) {
        if (exercises.isEmpty()) {
            Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
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

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(1f),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.add_exercise_btn),
            ),
            style = ButtonStyle.Primary,
            onClick = contract::onAddExercise
        )
    }
}