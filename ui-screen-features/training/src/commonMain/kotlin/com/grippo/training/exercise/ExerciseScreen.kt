package com.grippo.training.exercise

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.swipe.SwipeToReveal
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_set_btn
import com.grippo.design.resources.provider.exercise_record
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.sets_value
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

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
                        ),
                    style = ExerciseExampleCardStyle.Small(
                        onClick = contract::onExampleClick,
                    ),
                    value = example
                )
            }
        },
        trailing = {
            val buttonState = remember(loaders, state.exercise.iterations) {
                when {
                    state.exercise.iterations.isEmpty() -> ButtonState.Disabled
                    else -> ButtonState.Enabled
                }
            }

            Button(
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.save_btn),
                ),
                style = ButtonStyle.Primary,
                state = buttonState,
                size = ButtonSize.Small,
                onClick = contract::onSave
            )
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
                                    .animateItem()
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
                                modifier = Modifier.animateItem(),
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
                                        onRepetitionClick = editRepetitionProvider
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

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.add_set_btn),
                ),
                style = ButtonStyle.Secondary,
                onClick = contract::onAddIteration
            )

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
                exerciseExample = stubExerciseExample()
            ),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty
        )
    }
}
