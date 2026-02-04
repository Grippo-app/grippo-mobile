package com.grippo.iteration.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import kotlinx.collections.immutable.toPersistentList

public class IterationPickerViewModel(
    initial: IterationState,
    example: ExerciseExampleState,
    suggestions: List<IterationState>,
    number: Int,
    focus: IterationFocusState,
    private val dialogController: DialogController
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        value = initial,
        number = number,
        suggestions = suggestions.toPersistentList(),
        focus = focus,
        example = example,
    )
), IterationPickerContract {

    override fun onWeightPickerClick() {
        val weight = WeightFormatState.of(state.value.value.bodyWeight.value)

        val dialog = DialogConfig.WeightPicker(
            initial = weight,
            onResult = { result ->
                update { s ->
                    val iteration = s.value.copy(bodyWeight = result)
                    s.copy(value = iteration)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onExternalWeightChange(value: String) {
        update {
            val iteration = it.value.copy(externalWeight = VolumeFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onAssistWeightChange(value: String) {
        update {
            val iteration = it.value.copy(assistWeight = VolumeFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onExtraWeightChange(value: String) {
        update {
            val iteration = it.value.copy(extraWeight = VolumeFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onRepetitionsChange(value: String) {
        update {
            val iteration = it.value.copy(repetitions = RepetitionsFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onIterationClick(id: String) {
        update {
            val selected = it.suggestions.find { f -> f.id == id } ?: return@update it

            val iteration = it.value.copy(
                externalWeight = selected.externalWeight,
                bodyWeight = selected.bodyWeight,
                bodyMultiplier = selected.bodyMultiplier,
                assistWeight = selected.assistWeight,
                extraWeight = selected.extraWeight,
                repetitions = selected.repetitions
            )

            it.copy(value = iteration)
        }
    }

    override fun onSubmit() {
        val direction = IterationPickerDirection.BackWithResult(
            value = state.value.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(IterationPickerDirection.Back)
    }
}