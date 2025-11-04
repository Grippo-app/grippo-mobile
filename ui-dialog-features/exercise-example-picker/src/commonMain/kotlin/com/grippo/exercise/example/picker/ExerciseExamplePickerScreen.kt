package com.grippo.exercise.example.picker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.frames.BottomOverlayLazyColumn
import com.grippo.design.components.placeholder.ScreenPlaceholder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.ai_suggestion_btn
import com.grippo.design.resources.provider.icons.Magic
import com.grippo.design.resources.provider.not_found
import com.grippo.design.resources.provider.select_exercise
import com.grippo.exercise.example.picker.internal.AiSuggestionHeader
import com.grippo.exercise.example.picker.internal.ManualHeader
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExamplePickerScreen(
    state: ExerciseExamplePickerState,
    loaders: ImmutableSet<ExerciseExamplePickerLoader>,
    contract: ExerciseExamplePickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.select_exercise),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        AnimatedContent(
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            targetState = state.suggestion,
        ) { suggestion ->

            when (suggestion) {
                null -> ManualHeader(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.manual,
                    contract = contract,
                )

                else -> AiSuggestionHeader(
                    modifier = Modifier.fillMaxWidth(),
                    value = suggestion,
                    contract = contract
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        AnimatedContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            targetState = state.exerciseExamples.isEmpty(),
        ) {
            when (it) {
                true -> ScreenPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    text = AppTokens.strings.res(Res.string.not_found),
                )

                false -> BottomOverlayLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
                    content = {
                        items(
                            items = state.exerciseExamples,
                            key = { k -> k.value.id },
                        ) { item ->
                            val selectClickProvider = remember(item.value.id) {
                                { contract.onExerciseExampleSelectClick(item.value.id) }
                            }

                            ExerciseExampleCard(
                                modifier = Modifier.fillMaxWidth(),
                                value = item,
                                style = ExerciseExampleCardStyle.Medium(
                                    onCardClick = selectClickProvider,
                                ),
                            )
                        }
                    },
                    bottom = {
                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                        val buttonState = remember(loaders) {
                            when {
                                loaders.contains(ExerciseExamplePickerLoader.SuggestExample) -> ButtonState.Loading
                                else -> ButtonState.Enabled
                            }
                        }

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                            content = ButtonContent.Text(
                                startIcon = AppTokens.icons.Magic,
                                text = AppTokens.strings.res(Res.string.ai_suggestion_btn)
                            ),
                            state = buttonState,
                            style = ButtonStyle.Magic,
                            onClick = contract::onSuggestClick
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreeManualPreview() {
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample()
                ),
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExamplePickerContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreeSuggestionPreview() {
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample()
                ),
                suggestion = AiSuggestionQueries(
                    id = stubExerciseExample().value.id,
                    name = stubExerciseExample().value.name,
                    reason = "For some reason to change something"
                )
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExamplePickerContract.Empty
        )
    }
}
