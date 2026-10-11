package com.grippo.iteration.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import kotlinx.collections.immutable.toPersistentList

public class IterationPickerViewModel(
    initial: IterationState,
    example: ExerciseExampleState,
    iterationSuggestions: List<IterationState>,
    number: Int,
    focus: IterationFocusState,
    previousIteration: IterationState?,
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
        val repetitionSuggestions = (iterationSuggestions.mapNotNull {
            (it.repetitions as? RepetitionsFormatState.Valid)?.value
        } + listOf(6, 8, 10, 12)).distinct().take(5).toPersistentList()
        update {
            it.copy(
                repetitionSuggestions = repetitionSuggestions
            )
        }
    }

    override fun onWeightPickerClick() {
        navigateTo(IterationPickerDirection.PickBodyWeight(state.value.value.bodyWeight.value))
    }

    public fun onBodyWeightSelected(value: Float) {
        update { current ->
            val iteration = current.value.copy(bodyWeight = WeightFormatState.of(value))
            current.copy(value = iteration)
        }
    }

    override fun onExternalWeightChange(value: String) {
        update {
            val iteration = it.value.copy(externalWeight = parseWeight(value))
            it.copy(value = iteration)
        }
    }

    override fun onAssistWeightChange(value: String) {
        update {
            val iteration = it.value.copy(assistWeight = parseWeight(value))
            it.copy(value = iteration)
        }
    }

    override fun onExtraWeightChange(value: String) {
        update {
            val iteration = it.value.copy(extraWeight = parseWeight(value))
            it.copy(value = iteration)
        }
    }

    override fun onRepetitionsChange(value: String) {
        update {
            val iteration = it.value.copy(repetitions = RepetitionsFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onPreviousIterationClick() {
        val selected = state.value.previousIteration ?: return
        applyIteration(selected)
    }

    override fun onIterationClick(id: String) {
        val selected = state.value.iterationSuggestions.find { it.id == id } ?: return
        applyIteration(selected)
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
        navigateTo(IterationPickerDirection.Back)
    }
}
