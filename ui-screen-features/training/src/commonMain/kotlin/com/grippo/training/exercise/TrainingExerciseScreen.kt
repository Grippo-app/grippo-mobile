package com.grippo.training.exercise

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.dragdrop.DraggableItem
import com.grippo.design.components.dragdrop.rememberDragDropListState
import com.grippo.design.components.empty.IllustratedEmptyState
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.swipe.SwipeRevealAction
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_set_btn
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.empty_sets
import com.grippo.design.resources.provider.exercise_record
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.icons.Trash
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.sets_value
import com.grippo.design.resources.provider.training_first_set_hint
import com.grippo.design.resources.provider.training_no_sets
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun TrainingExerciseScreen(
    state: TrainingExerciseState,
    contract: TrainingExerciseContract,
) = BaseComposeScreen(
    background = ScreenBackground.Color(value = AppTokens.colors.background.screen),
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.exercise_record),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(onClick = contract::onBack),
        content = {
            state.exerciseExample?.let { example ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppTokens.dp.screen.horizontalPadding,
                            vertical = AppTokens.dp.contentPadding.content,
                        ),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                ) {
                    ExerciseExampleCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scalableClick(onClick = contract::onExampleClick),
                        style = ExerciseExampleCardStyle.Medium(value = example),
                    )
                    TrainingExerciseLoadType(components = example.components)
                }
            }

            HorizontalDivider(color = AppTokens.colors.divider.default)
        },
    )

    val basePadding = PaddingValues(top = AppTokens.dp.contentPadding.block)

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            AnimatedContent(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90))
                        .togetherWith(fadeOut(animationSpec = tween(durationMillis = 90)))
                },
                targetState = state.exercise.iterations.isEmpty(),
            ) { isEmpty ->
                when (isEmpty) {
                    true -> IllustratedEmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                        painter = AppTokens.drawables.res(Res.drawable.empty_sets),
                        illustrationStyle = IllustrationStyle.Medium,
                        title = AppTokens.strings.res(Res.string.training_no_sets),
                        text = AppTokens.strings.res(Res.string.training_first_set_hint),
                        contentPadding = resolvedPadding,
                    )

                    false -> {
                        val dragDropState = rememberDragDropListState(
                            onMove = contract::onStartReorderIterations,
                            onDragEnd = contract::onEndReorderIterations,
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = dragDropState.listState,
                            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                            contentPadding = resolvedPadding,
                        ) {
                            item {
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                                        .fillMaxWidth(),
                                    text = AppTokens.strings.res(
                                        Res.string.sets_value,
                                        state.exercise.iterations.size
                                    ),
                                    style = AppTokens.typography.h4(),
                                    color = AppTokens.colors.text.primary,
                                    textAlign = TextAlign.Start,
                                )
                            }

                            itemsIndexed(
                                items = state.exercise.iterations,
                                key = { _, item -> item.id },
                            ) { index, iteration ->
                                DraggableItem(state = dragDropState, key = iteration.id) {
                                    SwipeToReveal(
                                        horizontalPadding = AppTokens.dp.screen.horizontalPadding,
                                        actions = {
                                            SwipeRevealAction(
                                                label = AppTokens.strings.res(Res.string.delete_btn),
                                                icon = AppTokens.icons.Trash,
                                                onClick = { contract.onDeleteIteration(iteration.id) },
                                            )
                                        },
                                    ) {
                                        TrainingExerciseSetRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = iteration,
                                            number = index + 1,
                                            previousIteration = state.previousIterations.getOrNull(index),
                                            unusualWeight = iteration.id in state.volumeArtifactIds,
                                            unusualRepetitions = iteration.id in state.repetitionArtifactIds,
                                            onVolumeClick = { contract.onEditVolume(iteration.id) },
                                            onRepetitionClick = { contract.onEditRepetition(iteration.id) },
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
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.add_set_btn)),
                    style = ButtonStyle.Secondary,
                    onClick = contract::onAddIteration,
                )

                AnimatedVisibility(
                    visible = state.exercise.iterations.isNotEmpty(),
                    enter = expandHorizontally(
                        animationSpec = tween(durationMillis = 250),
                        expandFrom = Alignment.Start,
                    ) + slideInHorizontally(
                        animationSpec = tween(durationMillis = 250),
                        initialOffsetX = { it },
                    ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = shrinkHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        shrinkTowards = Alignment.Start,
                    ) + slideOutHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        targetOffsetX = { it },
                    ) + fadeOut(animationSpec = tween(durationMillis = 200)),
                ) {
                    Button(
                        modifier = Modifier.padding(start = AppTokens.dp.contentPadding.content),
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.save_btn),
                            startIcon = ButtonIcon.Icon(AppTokens.icons.Check),
                        ),
                        style = ButtonStyle.Primary,
                        state = ButtonState.Enabled,
                        onClick = contract::onSave,
                    )
                }
            }

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

private fun trainingExercisePreviewState(): TrainingExerciseState {
    val example = stubExerciseExample()
    val exerciseExample = example.copy(
        value = example.value.copy(
            id = "training-exercise-example",
            name = "Жим гантелей лежачи на похилій лаві нейтральним хватом",
        ),
        components = ExerciseExampleComponentsState.External(required = true),
    )
    val firstIteration = stubPendingIteration().copy(
        id = "training-exercise-set-1",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.of(10),
    )
    val secondIteration = firstIteration.copy(
        id = "training-exercise-set-2",
        externalWeight = VolumeFormatState.of(65f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val thirdIteration = secondIteration.copy(
        id = "training-exercise-set-3",
        repetitions = RepetitionsFormatState.of(7),
    )
    val fourthIteration = thirdIteration.copy(
        id = "training-exercise-set-4",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val previousIteration = firstIteration.copy(
        id = "training-exercise-previous-set",
        externalWeight = VolumeFormatState.of(55f),
    )
    return TrainingExerciseState(
        exercise = stubExercise().copy(
            id = "training-exercise",
            name = exerciseExample.value.name,
            exerciseExample = exerciseExample.value,
            iterations = persistentListOf(firstIteration, secondIteration, thirdIteration, fourthIteration),
            createdAt = DateTimeFormatState.Valid(
                display = "16 Oct 2026",
                value = LocalDateTime(2026, 10, 16, 12, 0),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
        ),
        exerciseExample = exerciseExample,
        previousIterations = persistentListOf(previousIteration),
    )
}

@AppPreview
@Composable
private fun TrainingExerciseScreenPopulatedPreview() {
    PreviewContainer {
        TrainingExerciseScreen(
            state = trainingExercisePreviewState(),
            contract = TrainingExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseScreenWarningsPreview() {
    val populated = trainingExercisePreviewState()
    val iteration = populated.exercise.iterations.first().copy(
        externalWeight = VolumeFormatState.of(600f),
        repetitions = RepetitionsFormatState.of(100),
    )
    PreviewContainer {
        TrainingExerciseScreen(
            state = populated.copy(
                exercise = populated.exercise.copy(iterations = persistentListOf(iteration)),
                volumeArtifactIds = persistentSetOf(iteration.id),
                repetitionArtifactIds = persistentSetOf(iteration.id),
            ),
            contract = TrainingExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseScreenWithoutExamplePreview() {
    PreviewContainer {
        TrainingExerciseScreen(
            state = trainingExercisePreviewState().copy(exerciseExample = null),
            contract = TrainingExerciseContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingExerciseScreenEmptyPreview() {
    val populated = trainingExercisePreviewState()
    PreviewContainer {
        TrainingExerciseScreen(
            state = populated.copy(
                exercise = populated.exercise.copy(iterations = persistentListOf()),
                previousIterations = persistentListOf(),
            ),
            contract = TrainingExerciseContract.Empty,
        )
    }
}
