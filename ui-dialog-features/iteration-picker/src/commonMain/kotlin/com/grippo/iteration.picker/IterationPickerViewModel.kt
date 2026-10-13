package com.grippo.iteration.picker

import com.grippo.analytics.iteration.picker.IterationPickerAnalytics
import com.grippo.analytics.iteration.picker.models.IterationPickerEvent
import com.grippo.analytics.iteration.picker.models.IterationPickerField
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.iteration.picker.analytics.inputStatus
import kotlinx.collections.immutable.toPersistentList

public class IterationPickerViewModel(
    initial: IterationState,
    example: ExerciseExampleState,
    iterationSuggestions: List<IterationState>,
    number: Int,
    focus: IterationFocusState,
    previousIteration: IterationState?,
    private val analytics: IterationPickerAnalytics,
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        value = initial,
        number = number,
        iterationSuggestions = iterationSuggestions.toPersistentList(),
        focus = focus,
        example = example,
        previousIteration = previousIteration,
    )
), IterationPickerContract {

    init {
        val repetitionSuggestions = (iterationSuggestions
            .mapNotNull { (it.repetitions as? RepetitionsFormatState.Valid)?.value } + listOf(6, 8, 10, 12))
            .distinct()
            .take(5)
            .toPersistentList()
        update { it.copy(repetitionSuggestions = repetitionSuggestions) }
    }

    override fun onWeightPickerClick() {
        analytics.trackEvent(IterationPickerEvent.BodyWeightOpened)
        navigateTo(IterationPickerDirection.PickBodyWeight(state.value.value.bodyWeight.value))
    }

    public fun onBodyWeightSelected(value: Float) {
        val selected = WeightFormatState.of(value)
        update { it.copy(value = it.value.copy(bodyWeight = selected)) }
        analytics.trackEvent(IterationPickerEvent.BodyWeightConfirmed(selected.inputStatus()))
    }

    override fun onExternalWeightChange(value: String) {
        val previous = state.value.value.externalWeight.inputStatus()
        val next = parseWeight(value)
        update { it.copy(value = it.value.copy(externalWeight = next)) }
        if (previous != next.inputStatus()) {
            analytics.trackEvent(IterationPickerEvent.InputChanged(IterationPickerField.ExternalWeight, next.inputStatus()))
        }
    }

    override fun onAssistWeightChange(value: String) {
        val previous = state.value.value.assistWeight.inputStatus()
        val next = parseWeight(value)
        update { it.copy(value = it.value.copy(assistWeight = next)) }
        if (previous != next.inputStatus()) {
            analytics.trackEvent(IterationPickerEvent.InputChanged(IterationPickerField.AssistWeight, next.inputStatus()))
        }
    }

    override fun onExtraWeightChange(value: String) {
        val previous = state.value.value.extraWeight.inputStatus()
        val next = parseWeight(value)
        update { it.copy(value = it.value.copy(extraWeight = next)) }
        if (previous != next.inputStatus()) {
            analytics.trackEvent(IterationPickerEvent.InputChanged(IterationPickerField.ExtraWeight, next.inputStatus()))
        }
    }

    override fun onRepetitionsChange(value: String) {
        val previous = state.value.value.repetitions.inputStatus()
        val next = RepetitionsFormatState.of(value)
        update { it.copy(value = it.value.copy(repetitions = next)) }
        if (previous != next.inputStatus()) {
            analytics.trackEvent(IterationPickerEvent.InputChanged(IterationPickerField.Repetitions, next.inputStatus()))
        }
    }

    override fun onPreviousIterationClick() {
        val selected = state.value.previousIteration ?: return
        analytics.trackEvent(IterationPickerEvent.PreviousIterationSelected)
        applyIteration(selected)
    }

    override fun onIterationClick(id: String) {
        val selected = state.value.iterationSuggestions.find { it.id == id } ?: return
        analytics.trackEvent(IterationPickerEvent.IterationSuggestionSelected)
        applyIteration(selected)
    }

    override fun onRepetitionSuggestionClick(value: Int) {
        if (value !in state.value.repetitionSuggestions) return
        analytics.trackEvent(IterationPickerEvent.RepetitionSuggestionSelected)
        update { it.copy(value = it.value.copy(repetitions = RepetitionsFormatState.of(value))) }
    }

    private fun applyIteration(selected: IterationState) {
        update {
            val components = it.example.components

            val externalWeight = when (components) {
                is ExerciseExampleComponentsState.External -> selected.externalWeight
                else -> VolumeFormatState.Empty()
            }
            val assistWeight = when (components) {
                is ExerciseExampleComponentsState.BodyAndAssist -> selected.assistWeight
                else -> VolumeFormatState.Empty()
            }
            val extraWeight = when (components) {
                is ExerciseExampleComponentsState.BodyAndExtra -> selected.extraWeight
                else -> VolumeFormatState.Empty()
            }

            val iteration = it.value.copy(
                externalWeight = externalWeight,
                assistWeight = assistWeight,
                extraWeight = extraWeight,
                repetitions = selected.repetitions
            )

            it.copy(value = iteration)
        }
    }

    override fun onSubmit() {
        if (!canSubmit(state.value.value, state.value.example.components)) return

        analytics.trackEvent(IterationPickerEvent.Submit)
        val direction = IterationPickerDirection.BackWithResult(
            value = state.value.value
        )
        navigateTo(direction)
    }

    private fun canSubmit(
        value: IterationState,
        components: ExerciseExampleComponentsState
    ): Boolean {
        if (value.repetitions !is RepetitionsFormatState.Valid) return false
        return when (components) {
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
    }

    private fun parseWeight(value: String): VolumeFormatState {
        val parsed = VolumeFormatState.of(value)
        return if (parsed is VolumeFormatState.Empty) parsed.copy(display = value) else parsed
    }

    override fun onBack() {
        analytics.trackEvent(IterationPickerEvent.Back)
        navigateTo(IterationPickerDirection.Back)
    }
}
