package com.grippo.training.recording.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.placeholder.ScreenPlaceholder
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_exercise_btn
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.no_exercises_yet
import com.grippo.training.recording.TrainingRecordingContract
import com.grippo.training.recording.TrainingRecordingScreen
import com.grippo.training.recording.TrainingRecordingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ColumnScope.ExercisesPage(
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
            true -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    ScreenPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        text = AppTokens.strings.res(Res.string.no_exercises_yet),
                    )

                    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                    Button(
                        modifier = Modifier
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                            .fillMaxWidth(1f),
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.add_exercise_btn),
                        ),
                        style = ButtonStyle.Secondary,
                        onClick = contract::onAddExercise
                    )

                    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }

            false -> {
                val basePadding = PaddingValues(top = AppTokens.dp.contentPadding.block)

                BottomOverlayContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = basePadding,
                    overlay = AppTokens.colors.background.screen,
                    content = { containerModifier, resolvedPadding ->
                        LazyColumn(
                            modifier = containerModifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
                            contentPadding = resolvedPadding
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
                                                icon = ButtonIcon.Icon(AppTokens.icons.Cancel)
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
                                        style = ExerciseCardStyle.Large(
                                            onClick = editExerciseProvider
                                        ),
                                    )
                                }
                            }
                        }
                    },
                    bottom = {
                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                        Button(
                            modifier = Modifier
                                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                                .fillMaxWidth(1f),
                            content = ButtonContent.Text(
                                text = AppTokens.strings.res(Res.string.add_exercise_btn),
                            ),
                            style = ButtonStyle.Secondary,
                            onClick = contract::onAddExercise
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                )
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
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}
