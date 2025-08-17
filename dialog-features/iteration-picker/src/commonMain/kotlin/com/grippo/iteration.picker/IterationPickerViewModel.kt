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

    override fun onWeightChange(value: Float) {
        update { it.copy(weight = value) }
    }

    override fun onRepetitionsChange(value: Int) {
        update { it.copy(repetitions = value) }
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