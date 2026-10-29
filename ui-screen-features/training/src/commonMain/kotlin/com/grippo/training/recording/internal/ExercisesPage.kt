package com.grippo.training.recording.internal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.design.components.dragdrop.DraggableItem
import com.grippo.design.components.dragdrop.rememberDragDropListState
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.empty.EmptyStateStyle
import com.grippo.design.components.swipe.SwipeRevealAction
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.design.resources.provider.empty_training_exercises
import com.grippo.design.resources.provider.icons.Trash
import com.grippo.training.recording.TrainingRecordingContract
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ExercisesPage(
    modifier: Modifier = Modifier,
    exercises: ImmutableList<ExerciseState>,
    examples: ImmutableList<ExerciseExampleState>,
    contentPadding: PaddingValues,
    contract: TrainingRecordingContract,
) {
    AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90))
                .togetherWith(fadeOut(animationSpec = tween(durationMillis = 90)))
        },
        targetState = exercises.isEmpty(),
    ) { isEmpty ->
        when (isEmpty) {
            true -> {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding),
                    text = AppTokens.strings.res(Res.string.empty_training_exercises),
                    style = EmptyStateStyle.Illustrated(
                        painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
                    ),
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
                    contentPadding = contentPadding,
                ) {
                    items(
                        items = exercises,
                        key = { exercise -> exercise.id },
                    ) { exercise ->
                        val name = examples
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
}
