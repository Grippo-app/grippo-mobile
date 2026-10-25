package com.grippo.training.recording.internal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.dragdrop.DraggableItem
import com.grippo.design.components.dragdrop.rememberDragDropListState
import com.grippo.design.components.empty.IllustratedEmptyState
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.swipe.SwipeRevealAction
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_exercise_btn
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.design.resources.provider.empty_training_exercises
import com.grippo.design.resources.provider.icons.Trash
import com.grippo.training.recording.TrainingRecordingContract
import com.grippo.training.recording.TrainingRecordingState

@Composable
internal fun ColumnScope.ExercisesPage(
    modifier: Modifier = Modifier,
    state: TrainingRecordingState,
    contract: TrainingRecordingContract,
) {
    val exercises = state.exercises.orEmpty()
    val basePadding = PaddingValues(top = AppTokens.dp.contentPadding.content)

    OverlayContainer(
        modifier = modifier,
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { resolvedPadding ->
            AnimatedContent(
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90))
                        .togetherWith(fadeOut(animationSpec = tween(durationMillis = 90)))
                },
                targetState = exercises.isEmpty(),
            ) { isEmpty ->
                when (isEmpty) {
                    true -> {
                        IllustratedEmptyState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                            painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
                            contentPadding = resolvedPadding,
                            text = AppTokens.strings.res(Res.string.empty_training_exercises),
                        )
                    }

                    false -> {
                        val dragDropState = rememberDragDropListState(
                            onMove = contract::onStartReorderExercises,
                            onDragEnd = contract::onEndReorderExercises,
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = dragDropState.listState,
                            contentPadding = resolvedPadding,
                        ) {
                            items(
                                items = exercises,
                                key = { exercise -> exercise.id },
                            ) { exercise ->
                                val name = state.examples
                                    .firstOrNull { it.value.id == exercise.exerciseExample.id }
                                    ?.value?.name ?: exercise.exerciseExample.name
                                val example = exercise.exerciseExample.copy(name = name)
                                val displayExercise = exercise.copy(exerciseExample = example)

                                DraggableItem(state = dragDropState, key = exercise.id) {
                                    SwipeToReveal(
                                        horizontalPadding = AppTokens.dp.screen.horizontalPadding,
                                        actions = {
                                            SwipeRevealAction(
                                                label = AppTokens.strings.res(Res.string.delete_btn),
                                                icon = AppTokens.icons.Trash,
                                                onClick = {
                                                    contract.onDeleteExercise(exercise.id)
                                                },
                                            )
                                        }
                                    ) {
                                        ExerciseCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = displayExercise,
                                            style = ExerciseCardStyle.Large(
                                                onClick = {
                                                    contract.onEditExercise(exercise.id)
                                                },
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottom = {
            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.add_exercise_btn),
                ),
                style = ButtonStyle.Secondary,
                onClick = contract::onAddExercise,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}
