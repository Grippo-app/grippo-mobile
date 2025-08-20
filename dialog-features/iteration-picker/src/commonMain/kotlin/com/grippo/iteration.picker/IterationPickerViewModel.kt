package com.grippo.iteration.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.IterationFocus
import com.grippo.state.trainings.IterationState

public class IterationPickerViewModel(
    initial: IterationState,
    focus: IterationFocus
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        focus = focus,
        value = initial
    )
), IterationPickerContract {

    override fun onVolumeChange(value: String) {
        update {
            val iteration = it.value.copy(volume = VolumeFormatState.of(value))
            it.copy(value = iteration)
        }
    }

    override fun onRepetitionsChange(value: String) {
        update {
            val iteration = it.value.copy(repetitions = RepetitionsFormatState.of(value))
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