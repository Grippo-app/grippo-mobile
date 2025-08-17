package com.grippo.iteration.picker

import com.grippo.core.BaseViewModel

public class IterationPickerViewModel(
    volume: Float,
    repetitions: Int
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        volume = volume,
        repetitions = repetitions
    )
), IterationPickerContract {

    override fun onVolumeChange(value: Float) {
        update { it.copy(volume = value) }
    }

    override fun onRepetitionsChange(value: Int) {
        update { it.copy(repetitions = value) }
    }

    override fun onSubmit() {
        val direction = IterationPickerDirection.BackWithResult(
            volume = state.value.volume,
            repetitions = state.value.repetitions
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(IterationPickerDirection.Back)
    }
}