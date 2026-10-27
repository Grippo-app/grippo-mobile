package com.grippo.iteration.picker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.FormatState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipInteraction
import com.grippo.design.components.chip.ChipLeading
import com.grippo.design.components.inputs.InputRepetitions
import com.grippo.design.components.inputs.InputVolume
import com.grippo.design.components.inputs.InputWeight
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.assist_weight_placeholder
import com.grippo.design.resources.provider.body_weight_placeholder
import com.grippo.design.resources.provider.decimal_separator
import com.grippo.design.resources.provider.extra_weight_placeholder
import com.grippo.design.resources.provider.iteration_picker_suggestions
import com.grippo.design.resources.provider.iteration_picker_repetitions_range
import com.grippo.design.resources.provider.iteration_picker_weight_range
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.repetitions_placeholder
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.iteration_picker_last_time
import com.grippo.design.resources.provider.volume_placeholder
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

private const val SuggestionsAnimationDurationMillis = 200

@Composable
internal fun IterationPickerScreen(
    state: IterationPickerState,
    loaders: ImmutableSet<IterationPickerLoader>,
    contract: IterationPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        val weightRequester = remember { FocusRequester() }
        val repetitionsRequester = remember { FocusRequester() }
        var repetitionsFocused by remember { mutableStateOf(false) }

        LaunchedEffect(state.focus, state.example.components) {
            withFrameNanos { }

            delay(250.milliseconds)

            when (state.focus) {
                IterationFocusState.VOLUME -> {
                    if (state.example.components is ExerciseExampleComponentsState.BodyOnly) {
                        repetitionsRequester.requestFocus()
                    } else {
                        weightRequester.requestFocus()
                    }
                }

                IterationFocusState.REPETITIONS -> {
                    repetitionsRequester.requestFocus()
                }

                IterationFocusState.UNIDENTIFIED -> {}
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        val previousIteration = when (val components = state.example.components) {
            is ExerciseExampleComponentsState.BodyAndAssist -> state.previousIteration?.takeIf {
                it.assistWeight !is VolumeFormatState.Invalid &&
                    (!components.assistRequired || it.assistWeight is VolumeFormatState.Valid)
            }
            else -> state.previousIteration
        }
        val iterationSuggestions = remember(state.iterationSuggestions, previousIteration, state.example.components) {
            state.iterationSuggestions
                .filter { item ->
                    item.repetitions is RepetitionsFormatState.Valid && when (val components = state.example.components) {
                        is ExerciseExampleComponentsState.External -> item.externalWeight is VolumeFormatState.Valid
                        is ExerciseExampleComponentsState.BodyAndExtra -> item.extraWeight !is VolumeFormatState.Invalid
                        is ExerciseExampleComponentsState.BodyAndAssist ->
                            item.assistWeight !is VolumeFormatState.Invalid &&
                                (!components.assistRequired || item.assistWeight is VolumeFormatState.Valid)
                        is ExerciseExampleComponentsState.BodyOnly -> true
                    }
                }
                .distinctBy { it.suggestionKey(state.example.components) }
                .filter { item ->
                    item.suggestionKey(state.example.components) !=
                        previousIteration?.suggestionKey(state.example.components)
                }
        }
        AnimatedContent(
            modifier = Modifier.fillMaxWidth(),
            targetState = repetitionsFocused,
            contentAlignment = Alignment.TopStart,
            transitionSpec = {
                fadeIn(tween(SuggestionsAnimationDurationMillis)) togetherWith
                    fadeOut(tween(SuggestionsAnimationDurationMillis)) using SizeTransform(
                        clip = true,
                        sizeAnimationSpec = { _, _ -> tween(SuggestionsAnimationDurationMillis) },
                    )
            },
            label = "Iteration suggestions",
        ) { showRepetitionSuggestions ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showRepetitionSuggestions || previousIteration != null || iterationSuggestions.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                        text = AppTokens.strings.res(
                            if (showRepetitionSuggestions) Res.string.repetitions_placeholder
                            else Res.string.iteration_picker_suggestions
                        ),
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.tertiary
                    )
                    Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

                    key(showRepetitionSuggestions) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                        ) {
                            if (showRepetitionSuggestions) {
                                state.repetitionSuggestions.forEach { repetitions ->
                                    SuggestionChip(
                                        value = repetitions.toString(),
                                        isSelected = state.value.repetitions.value == repetitions,
                                        onClick = { contract.onRepetitionSuggestionClick(repetitions) }
                                    )
                                }
                            } else {
                                previousIteration?.let { previous ->
                                    SuggestionChip(
                                        value = AppTokens.strings.res(
                                            Res.string.iteration_picker_last_time,
                                            previous.suggestionLabel(state.example.components),
                                        ),
                                        isSelected = previous.suggestionKey(state.example.components) ==
                                            state.value.suggestionKey(state.example.components),
                                        onClick = contract::onPreviousIterationClick,
                                    )
                                }
                                iterationSuggestions.forEach { item ->
                                    key(item.id) {
                                        val clickProvider = remember(item.id, contract) {
                                            { contract.onIterationClick(item.id) }
                                        }

                                        val isSelected = item.suggestionKey(state.example.components) ==
                                            state.value.suggestionKey(state.example.components)

                                        SuggestionChip(
                                            value = item.suggestionLabel(state.example.components),
                                            isSelected = isSelected,
                                            onClick = clickProvider
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
                }
            }
        }

        when (state.example.components) {
            is ExerciseExampleComponentsState.BodyAndAssist,
            is ExerciseExampleComponentsState.BodyAndExtra,
            is ExerciseExampleComponentsState.BodyOnly -> {
                InputWeight(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                        .fillMaxWidth(),
                    onClick = contract::onWeightPickerClick,
                    placeholder = buildString {
                        append(AppTokens.strings.res(Res.string.body_weight_placeholder))
                        if (state.value.bodyMultiplier.value != null) {
                            append(" · ")
                            append(state.value.bodyMultiplier.short())
                        }
                    },
                    value = state.value.bodyWeight
                )
                Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
            }

            is ExerciseExampleComponentsState.External -> Unit
        }

        FlowRow(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            maxItemsInEachRow = if (LocalDensity.current.fontScale > 1.2f) 1 else 2,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {

            val weight = when (state.example.components) {
                is ExerciseExampleComponentsState.BodyAndAssist -> state.value.assistWeight
                is ExerciseExampleComponentsState.BodyAndExtra -> state.value.extraWeight
                is ExerciseExampleComponentsState.External -> state.value.externalWeight
                is ExerciseExampleComponentsState.BodyOnly -> null
            }
            if (weight != null) {
                Column(modifier = Modifier.weight(1f)) {
                    InputVolume(
                        modifier = Modifier.fillMaxWidth().focusRequester(weightRequester),
                        value = weight,
                        placeholder = AppTokens.strings.res(
                            when (state.example.components) {
                                is ExerciseExampleComponentsState.BodyAndAssist -> Res.string.assist_weight_placeholder
                                is ExerciseExampleComponentsState.BodyAndExtra -> Res.string.extra_weight_placeholder
                                else -> Res.string.volume_placeholder
                            }
                        ),
                        onValueChange = when (state.example.components) {
                            is ExerciseExampleComponentsState.BodyAndAssist -> contract::onAssistWeightChange
                            is ExerciseExampleComponentsState.BodyAndExtra -> contract::onExtraWeightChange
                            else -> contract::onExternalWeightChange
                        }
                    )
                    Spacer(Modifier.size(AppTokens.dp.contentPadding.text))
                    Text(
                        text = AppTokens.strings.res(
                            Res.string.iteration_picker_weight_range,
                            if (state.example.components is ExerciseExampleComponentsState.External) {
                                "0${AppTokens.strings.res(Res.string.decimal_separator)}1"
                            } else {
                                "0"
                            }
                        ),
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.tertiary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                InputRepetitions(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { repetitionsFocused = it.isFocused }
                        .focusRequester(repetitionsRequester),
                    value = state.value.repetitions,
                    onValueChange = contract::onRepetitionsChange,
                    onDone = contract::onSubmit
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.iteration_picker_repetitions_range),
                    style = AppTokens.typography.b12Semi(),
                    color = AppTokens.colors.text.tertiary,
                )
            }
        }

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        val buttonState = remember(state.value, state.example.components) {
            val value = state.value
            val validWeight = when (val components = state.example.components) {
                is ExerciseExampleComponentsState.External ->
                    value.externalWeight is VolumeFormatState.Valid
                is ExerciseExampleComponentsState.BodyOnly ->
                    value.bodyWeight is WeightFormatState.Valid &&
                        value.bodyMultiplier is MultiplierFormatState.Valid
                is ExerciseExampleComponentsState.BodyAndExtra ->
                    value.bodyWeight is WeightFormatState.Valid &&
                        value.bodyMultiplier is MultiplierFormatState.Valid &&
                        value.extraWeight !is VolumeFormatState.Invalid
                is ExerciseExampleComponentsState.BodyAndAssist ->
                    value.bodyWeight is WeightFormatState.Valid &&
                        value.bodyMultiplier is MultiplierFormatState.Valid &&
                        value.assistWeight !is VolumeFormatState.Invalid &&
                        (!components.assistRequired || value.assistWeight is VolumeFormatState.Valid)
            }
            if (value.repetitions is RepetitionsFormatState.Valid && validWeight) {
                ButtonState.Enabled
            } else {
                ButtonState.Disabled
            }
        }

        Button(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.save_btn)),
            style = ButtonStyle.Primary,
            state = buttonState,
            onClick = contract::onSubmit
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SuggestionChip(
    modifier: Modifier = Modifier,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = AppTokens.dp.input.height)
            .semantics { selected = isSelected }
            .scalableClick(haptic = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Chip(
            label = ChipLabel.Empty,
            interaction = ChipInteraction.Static,
            value = value,
            leading = ChipLeading.Empty,
            textColor = if (isSelected) AppTokens.colors.text.primary
                else AppTokens.colors.text.secondary,
            borderColor = if (isSelected) AppTokens.colors.selection.accent
                else AppTokens.colors.border.default,
            iconColor = AppTokens.colors.text.secondary,
            size = ChipSize.Medium,
            brush = SolidColor(
                if (isSelected) AppTokens.colors.selection.background
                else AppTokens.colors.background.card
            )
        )
    }
}

private fun IterationState.suggestionKey(
    components: ExerciseExampleComponentsState
): Pair<Float?, Int?> {
    val weight = when (components) {
        is ExerciseExampleComponentsState.External -> externalWeight.value
        is ExerciseExampleComponentsState.BodyAndExtra -> extraWeight.value ?: 0f
        is ExerciseExampleComponentsState.BodyAndAssist -> assistWeight.value ?: 0f
        is ExerciseExampleComponentsState.BodyOnly -> null
    }
    return weight to repetitions.value
}

@Composable
private fun IterationState.suggestionLabel(components: ExerciseExampleComponentsState): String {
    val zero = "0${FormatState.UNIT_SPACE}${AppTokens.strings.res(Res.string.kg)}"
    val weight = when (components) {
        is ExerciseExampleComponentsState.External -> externalWeight.short()
        is ExerciseExampleComponentsState.BodyAndExtra ->
            if (extraWeight.value == null) zero else "+${extraWeight.short()}"
        is ExerciseExampleComponentsState.BodyAndAssist ->
            if (assistWeight.value == null || assistWeight.value == 0f) zero
            else "−${assistWeight.short()}"
        is ExerciseExampleComponentsState.BodyOnly -> ""
    }
    return "${if (weight.isEmpty()) "" else "$weight "}× ${repetitions.value ?: "–"}"
}

@AppPreview
@Composable
private fun SuggestionChipsPreview() {
    PreviewContainer {
        SuggestionChip(value = "8", isSelected = false, onClick = {})
        SuggestionChip(value = "12", isSelected = true, onClick = {})
        SuggestionChip(value = "Минулого разу: 80 kg × 12", isSelected = true, onClick = {})
    }
}

@AppPreview
@Composable
private fun ExternalPreview() = PickerPreview(ExerciseExampleComponentsState.External(true))

@AppPreview
@Composable
private fun BodyPreview() = PickerPreview(ExerciseExampleComponentsState.BodyOnly(true, 0.65f))

@AppPreview
@Composable
private fun ExtraPreview() = PickerPreview(
    ExerciseExampleComponentsState.BodyAndExtra(true, 0.65f, false)
)

@AppPreview
@Composable
private fun AssistPreview() = PickerPreview(
    ExerciseExampleComponentsState.BodyAndAssist(true, 0.65f, false)
)

@AppPreview
@Composable
private fun RequiredAssistZeroPreview() = PickerPreview(
    components = ExerciseExampleComponentsState.BodyAndAssist(
        bodyRequired = true,
        bodyMultiplier = 1f,
        assistRequired = true,
    ),
    assistWeight = VolumeFormatState.of(value = 0f, allowZero = true),
)

@AppPreview
@Composable
private fun RequiredAssistMissingPreview() = PickerPreview(
    components = ExerciseExampleComponentsState.BodyAndAssist(
        bodyRequired = true,
        bodyMultiplier = 1f,
        assistRequired = true,
    ),
    assistWeight = VolumeFormatState.Empty(),
)

@AppPreview
@Composable
private fun RequiredAssistInvalidPreview() = PickerPreview(
    components = ExerciseExampleComponentsState.BodyAndAssist(
        bodyRequired = true,
        bodyMultiplier = 1f,
        assistRequired = true,
    ),
    assistWeight = VolumeFormatState.of(display = "-0.01", allowZero = true),
)

@AppPreview
@Composable
private fun WithoutSuggestionsPreview() {
    val example = stubExerciseExample()
    PreviewContainer {
        IterationPickerScreen(
            state = IterationPickerState(
                value = stubPendingIteration().copy(
                    id = "current-empty",
                    externalWeight = VolumeFormatState.Empty(),
                    extraWeight = VolumeFormatState.Empty(),
                    assistWeight = VolumeFormatState.Empty(),
                    bodyWeight = WeightFormatState.Empty(),
                    bodyMultiplier = MultiplierFormatState.Empty(),
                    repetitions = RepetitionsFormatState.Empty(),
                ),
                number = 1,
                iterationSuggestions = persistentListOf(),
                repetitionSuggestions = persistentListOf(6, 8, 10, 12),
                focus = IterationFocusState.UNIDENTIFIED,
                example = example.copy(
                    value = example.value.copy(id = "without-suggestions"),
                    components = ExerciseExampleComponentsState.External(true),
                ),
            ),
            loaders = persistentSetOf(),
            contract = IterationPickerContract.Empty,
        )
    }
}

@Composable
private fun PickerPreview(
    components: ExerciseExampleComponentsState,
    assistWeight: VolumeFormatState = VolumeFormatState.of(15f),
) {
    val bodyMultiplier = when (components) {
        is ExerciseExampleComponentsState.External -> MultiplierFormatState.Empty()
        is ExerciseExampleComponentsState.BodyOnly -> MultiplierFormatState.of(components.bodyMultiplier)
        is ExerciseExampleComponentsState.BodyAndExtra -> MultiplierFormatState.of(components.bodyMultiplier)
        is ExerciseExampleComponentsState.BodyAndAssist -> MultiplierFormatState.of(components.bodyMultiplier)
    }
    val value = IterationState(
        id = "current",
        externalWeight = if (components is ExerciseExampleComponentsState.External) {
            VolumeFormatState.of(80f)
        } else {
            VolumeFormatState.Empty()
        },
        extraWeight = if (components is ExerciseExampleComponentsState.BodyAndExtra) {
            VolumeFormatState.of(10f)
        } else {
            VolumeFormatState.Empty()
        },
        assistWeight = if (components is ExerciseExampleComponentsState.BodyAndAssist) {
            assistWeight
        } else {
            VolumeFormatState.Empty()
        },
        bodyWeight = if (components is ExerciseExampleComponentsState.External) {
            WeightFormatState.Empty()
        } else {
            WeightFormatState.of(78f)
        },
        bodyMultiplier = bodyMultiplier,
        repetitions = RepetitionsFormatState.of(8)
    )
    PreviewContainer {
        IterationPickerScreen(
            state = IterationPickerState(
                value = value,
                repetitionSuggestions = persistentListOf(8, 12, 6, 10),
                previousIteration = value.copy(id = "last-time", repetitions = RepetitionsFormatState.of(10)),
                number = 2,
                iterationSuggestions = persistentListOf(
                    value.copy(id = "previous-1"),
                    value.copy(id = "previous-2", repetitions = RepetitionsFormatState.of(12))
                ),
                focus = IterationFocusState.UNIDENTIFIED,
                example = stubExerciseExample().copy(components = components)
            ),
            loaders = persistentSetOf(),
            contract = IterationPickerContract.Empty
        )
    }
}
