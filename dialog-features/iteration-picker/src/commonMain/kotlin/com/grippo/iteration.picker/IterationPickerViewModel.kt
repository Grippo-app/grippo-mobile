package com.grippo.iteration.picker

import com.grippo.core.BaseViewModel

public class IterationPickerViewModel(
    weight: Float,
    repetitions: Int
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        weight = weight,
        repetitions = repetitions
    )
), IterationPickerContract {

    override fun onWeightChange(value: String) {
        val weight = value.toFloatOrNull() ?: return
        update { it.copy(weight = weight) }
    }

    override fun onRepetitionsChange(value: String) {
        val repetitions = value.toIntOrNull() ?: return
        update { it.copy(repetitions = repetitions) }
    }

    override fun onSubmit() {
        val direction = IterationPickerDirection.BackWithResult(
            weight = state.value.weight,
            repetitions = state.value.repetitions
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(IterationPickerDirection.Back)
    }
}


