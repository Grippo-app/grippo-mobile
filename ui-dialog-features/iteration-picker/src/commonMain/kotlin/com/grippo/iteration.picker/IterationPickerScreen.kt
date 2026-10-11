package com.grippo.iteration.picker

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
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
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
import com.grippo.design.resources.provider.extra_weight_placeholder
import com.grippo.design.resources.provider.iteration_picker_previous_sets
import com.grippo.design.resources.provider.iteration_picker_repetitions_range
import com.grippo.design.resources.provider.iteration_picker_weight_range
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.repetitions_placeholder
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.volume_placeholder
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

@Composable
internal fun IterationPickerScreen(
    state: IterationPickerState,
    contract: IterationPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        val weightRequester = remember { FocusRequester() }
        val repetitionsRequester = remember { FocusRequester() }
        var repetitionsFocused by remember { mutableStateOf(false) }

        LaunchedEffect(state.focus, state.example.components) {
            // run after composition
            withFrameNanos { /* no-op, just await a frame */ }

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

        Text(
            modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            text = state.example.value.name,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary
        )

        val iterationSuggestions = remember(state.iterationSuggestions, state.example.components) {
            state.iterationSuggestions
                .filter { item ->
                    item.repetitions is RepetitionsFormatState.Valid && when (state.example.components) {
                        is ExerciseExampleComponentsState.External -> item.externalWeight is VolumeFormatState.Valid
                        is ExerciseExampleComponentsState.BodyAndExtra -> item.extraWeight !is VolumeFormatState.Invalid
                        is ExerciseExampleComponentsState.BodyAndAssist -> item.assistWeight !is VolumeFormatState.Invalid
                        is ExerciseExampleComponentsState.BodyOnly -> true
                    }
                }
                .distinctBy { it.suggestionKey(state.example.components) }
        }
        if (repetitionsFocused || iterationSuggestions.isNotEmpty()) {
            Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
            Text(
                modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                text = AppTokens.strings.res(
                    if (repetitionsFocused) Res.string.repetitions_placeholder
                    else Res.string.iteration_picker_previous_sets
                ),
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.tertiary
            )
            Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

            key(repetitionsFocused) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                ) {
                    if (repetitionsFocused) {
                        state.repetitionSuggestions.forEach { repetitions ->
                            SuggestionChip(
                                value = repetitions.toString(),
                                isSelected = state.value.repetitions.value == repetitions,
                                onClick = { contract.onRepetitionsChange(repetitions.toString()) }
                            )
                        }
                    } else {
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
        }

        Spacer(Modifier.size(AppTokens.dp.contentPadding.content))

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
                            if (state.example.components is ExerciseExampleComponentsState.External) "0.1" else "0"
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
            val validWeight = when (state.example.components) {
                is ExerciseExampleComponentsState.External ->
                    value.externalWeight is VolumeFormatState.Valid
                is ExerciseExampleComponentsState.BodyOnly ->
                    value.bodyWeight is WeightFormatState.Valid
                is ExerciseExampleComponentsState.BodyAndExtra ->
                    value.bodyWeight is WeightFormatState.Valid &&
                        value.extraWeight !is VolumeFormatState.Invalid
                is ExerciseExampleComponentsState.BodyAndAssist ->
                    value.bodyWeight is WeightFormatState.Valid &&
                        value.assistWeight !is VolumeFormatState.Invalid
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
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
            stype = ChipStype.Default,
            value = value,
            trailing = ChipTrailing.Empty,
            textColor = if (isSelected) AppTokens.colors.border.focus
                else AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.text.secondary,
            size = ChipSize.Medium,
            brush = SolidColor(
                if (isSelected) AppTokens.colors.border.focus.copy(alpha = 0.12f)
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
    val kg = AppTokens.strings.res(Res.string.kg)
    val zero = "0 $kg"
    val weight = when (components) {
        is ExerciseExampleComponentsState.External -> "${externalWeight.short().removeSuffix(kg)} $kg"
        is ExerciseExampleComponentsState.BodyAndExtra ->
            if (extraWeight.value == null) zero else "+${extraWeight.short().removeSuffix(kg)} $kg"
        is ExerciseExampleComponentsState.BodyAndAssist ->
            if (assistWeight.value == null) zero else "−${assistWeight.short().removeSuffix(kg)} $kg"
        is ExerciseExampleComponentsState.BodyOnly -> ""
    }
    return "${if (weight.isEmpty()) "" else "$weight "}× ${repetitions.value ?: "–"}"
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

@Composable
private fun PickerPreview(components: ExerciseExampleComponentsState) {
    val value = IterationState(
        id = "current",
        externalWeight = VolumeFormatState.of(80f),
        extraWeight = VolumeFormatState.of(10f),
        assistWeight = VolumeFormatState.of(15f),
        bodyWeight = WeightFormatState.of(78f),
        bodyMultiplier = MultiplierFormatState.of(0.65f),
        repetitions = RepetitionsFormatState.of(8)
    )
    PreviewContainer {
        IterationPickerScreen(
            state = IterationPickerState(
                value = value,
                repetitionSuggestions = persistentListOf(8, 12, 6, 10),
                number = 2,
                iterationSuggestions = persistentListOf(
                    value.copy(id = "previous-1"),
                    value.copy(id = "previous-2", repetitions = RepetitionsFormatState.of(12))
                ),
                focus = IterationFocusState.UNIDENTIFIED,
                example = stubExerciseExample().copy(components = components)
            ),
            contract = IterationPickerContract.Empty
        )
    }
}
