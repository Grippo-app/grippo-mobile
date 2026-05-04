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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.components.badge.Badge
import com.grippo.design.components.badge.BadgeStyle
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.tooltip.Tooltip
import com.grippo.design.components.tooltip.TooltipContent
import com.grippo.design.components.tooltip.TooltipPlacement
import com.grippo.design.components.tooltip.TooltipVariant
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_set_btn
import com.grippo.design.resources.provider.empty_exercise_sets
import com.grippo.design.resources.provider.exercise_record
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.icons.EmptyExercise
import com.grippo.design.resources.provider.sets_value
import com.grippo.design.resources.provider.tooltip_suspicious_reps_subtitle
import com.grippo.design.resources.provider.tooltip_suspicious_reps_title
import com.grippo.design.resources.provider.tooltip_suspicious_weight_subtitle
import com.grippo.design.resources.provider.tooltip_suspicious_weight_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

@Composable
internal fun ExerciseScreen(
    state: ExerciseState,
    loaders: ImmutableSet<ExerciseLoader>,
    contract: ExerciseContract
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
                ExerciseExampleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = AppTokens.dp.screen.horizontalPadding,
                            end = AppTokens.dp.screen.horizontalPadding,
                            bottom = AppTokens.dp.contentPadding.block,
                            top = AppTokens.dp.contentPadding.content,
                        )
                        .scalableClick(onClick = contract::onExampleClick),
                    style = ExerciseExampleCardStyle.Medium(value = example),
                )
            }
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
                    true -> EmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        value = AppTokens.icons.EmptyExercise,
                        text = AppTokens.strings.res(Res.string.empty_exercise_sets)
                    )

                    false -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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

                            SwipeToReveal(
                                modifier = Modifier,
                                actions = {
                                    Button(
                                        modifier = Modifier.padding(end = AppTokens.dp.screen.horizontalPadding),
                                        content = ButtonContent.Icon(
                                            icon = ButtonIcon.Icon(AppTokens.icons.Cancel)
                                        ),
                                        style = ButtonStyle.Error,
                                        onClick = deleteIterationProvider
                                    )
                                }
                            ) {
                                IterationCard(
                                    modifier = Modifier
                                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                                        .fillMaxWidth(),
                                    value = iteration,
                                    style = IterationCardStyle.Editable(
                                        label = (index + 1).toString(),
                                        onVolumeClick = editVolumeProvider,
                                        onRepetitionClick = editRepetitionProvider,
                                        volumeDecorator = {
                                            if (iteration.id in state.volumeArtifactIds) {
                                                val tooltipState = rememberTooltipState()
                                                val scope = rememberCoroutineScope()

                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = AppTokens.dp.contentPadding.subContent)
                                                        .align(Alignment.CenterEnd),
                                                ) {
                                                    Tooltip(
                                                        state = tooltipState,
                                                        tooltipContent = TooltipContent.Rich(
                                                            title = AppTokens.strings.res(Res.string.tooltip_suspicious_weight_title),
                                                            subtitle = AppTokens.strings.res(Res.string.tooltip_suspicious_weight_subtitle),
                                                        ),
                                                        placement = TooltipPlacement.Top,
                                                        variant = TooltipVariant.Warning,
                                                    ) {
                                                        Badge(
                                                            style = BadgeStyle.Warning,
                                                            onClick = { scope.launch { tooltipState.show() } }
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        repetitionDecorator = {
                                            if (iteration.id in state.repetitionArtifactIds) {
                                                val tooltipState = rememberTooltipState()
                                                val scope = rememberCoroutineScope()

                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = AppTokens.dp.contentPadding.subContent)
                                                        .align(Alignment.CenterEnd),
                                                ) {
                                                    Tooltip(
                                                        state = tooltipState,
                                                        tooltipContent = TooltipContent.Rich(
                                                            title = AppTokens.strings.res(Res.string.tooltip_suspicious_reps_title),
                                                            subtitle = AppTokens.strings.res(Res.string.tooltip_suspicious_reps_subtitle),
                                                        ),
                                                        placement = TooltipPlacement.Start,
                                                        variant = TooltipVariant.Warning,
                                                    ) {
                                                        Badge(
                                                            style = BadgeStyle.Warning,
                                                            onClick = { scope.launch { tooltipState.show() } }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                )
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

                val buttonVisible = remember(state.exercise.iterations) {
                    state.exercise.iterations.isNotEmpty()
                }

                AnimatedVisibility(
                    visible = buttonVisible,
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
                        content = ButtonContent.Icon(
                            icon = ButtonIcon.Icon(AppTokens.icons.Check),
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
private fun ExerciseScreenPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(
                exercise = stubExercise(),
                exerciseExample = stubExerciseExample(),
                volumeArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
                repetitionArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
            ),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ExerciseScreenEmptyPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(
                exercise = stubExercise().copy(iterations = persistentListOf()),
                exerciseExample = stubExerciseExample(),
                volumeArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
                repetitionArtifactIds = persistentSetOf(stubExercise().iterations.random().id),
            ),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty
        )
    }
}
