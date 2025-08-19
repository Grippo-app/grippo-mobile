package com.grippo.iteration.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState

public class IterationPickerViewModel(
    volume: Float,
    repetitions: Int
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        volume = VolumeFormatState.of(volume),
        repetitions = RepetitionsFormatState.of(repetitions)
    )
), IterationPickerContract {

    override fun onVolumeChange(value: Float) {
        update { it.copy(volume = VolumeFormatState.of(value)) }
    }

    override fun onRepetitionsChange(value: Int) {
        update { it.copy(repetitions = RepetitionsFormatState.of(value)) }
    }

    override fun onSubmit() {
        val direction = IterationPickerDirection.BackWithResult(
            volume = state.value.volume.value,
            repetitions = state.value.repetitions.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(IterationPickerDirection.Back)
    }
}