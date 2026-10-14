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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.trainings.stubExercise
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingExerciseScreen(
    state: TrainingExerciseState,
    contract: TrainingExerciseContract,
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        leading = Leading.Back(contract::onBack),
        style = ToolbarStyle.Transparent,
        content = {
            state.exerciseExample?.let { example ->
                val shape = RoundedCornerShape(AppTokens.dp.iterationCard.radius)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppTokens.dp.screen.horizontalPadding,
                            vertical = AppTokens.dp.contentPadding.content,
                        )
                        .background(AppTokens.colors.background.card, shape)
                        .border(
                            AppTokens.dp.iterationCard.borderWidth,
                            AppTokens.colors.border.default,
                            shape
                        )
                        .padding(AppTokens.dp.contentPadding.content),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    ExerciseExampleCard(
                        modifier = Modifier.fillMaxWidth()
                            .scalableClick(onClick = contract::onExampleClick),
                        style = ExerciseExampleCardStyle.Medium(value = example),
                    )
                    TrainingExerciseLoadType(components = example.components)
                }
            }

            HorizontalDivider(color = AppTokens.colors.divider.default)
        },
        title = AppTokens.strings.res(Res.string.exercise_record),
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
                    (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                },
                targetState = state.exercise.iterations.isEmpty()
            ) {
                when (it) {
                    true -> IllustratedEmptyState(
                        modifier = Modifier.fillMaxSize()
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
                            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                            contentPadding = resolvedPadding
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
                                    textAlign = TextAlign.Start
                                )
                            }

                            itemsIndexed(
                                items = state.exercise.iterations,
                                key = { _, item -> item.id }
                            ) { index, iteration ->
                                val editVolumeProvider = remember(iteration.id) {
                                    { contract.onEditVolume(iteration.id) }
                                }

                                val editRepetitionProvider = remember(iteration.id) {
                                    { contract.onEditRepetition(iteration.id) }
                                }

                                val deleteIterationProvider = remember(iteration.id) {
                                    { contract.onDeleteIteration(iteration.id) }
                                }

                                DraggableItem(state = dragDropState, key = iteration.id) {
                                    SwipeToReveal(
                                        horizontalPadding = AppTokens.dp.screen.horizontalPadding,
                                        actions = {
                                            SwipeRevealAction(
                                                label = AppTokens.strings.res(Res.string.delete_btn),
                                                icon = AppTokens.icons.Trash,
                                                onClick = deleteIterationProvider
                                            )
                                        }
                                    ) {
                                        TrainingExerciseSetCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = iteration,
                                            number = index + 1,
                                            previousIteration = state.previousIterations.getOrNull(index),
                                            onVolumeClick = editVolumeProvider,
                                            onRepetitionClick = editRepetitionProvider,
                                            unusualWeight = iteration.id in state.volumeArtifactIds,
                                            unusualRepetitions = iteration.id in state.repetitionArtifactIds,
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
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.add_set_btn)),
                    style = ButtonStyle.Secondary,
                    onClick = contract::onAddIteration
                )

                AnimatedVisibility(
                    visible = state.exercise.iterations.isNotEmpty(),
                    enter = expandHorizontally(
                        animationSpec = tween(durationMillis = 250),
                        expandFrom = Alignment.Start
                    ) + slideInHorizontally(
                        animationSpec = tween(durationMillis = 250),
                        initialOffsetX = { it }
                    ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = shrinkHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        shrinkTowards = Alignment.Start
                    ) + slideOutHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        targetOffsetX = { it }
                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    Button(
                        modifier = Modifier.padding(start = AppTokens.dp.contentPadding.content),
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.save_btn),
                            startIcon = ButtonIcon.Icon(AppTokens.icons.Check),
                        ),
                        style = ButtonStyle.Primary,
                        state = ButtonState.Enabled,
                        onClick = contract::onSave
                    )
                }
            }

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
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
            contract = TrainingExerciseContract.Empty,
        )
    }
}
