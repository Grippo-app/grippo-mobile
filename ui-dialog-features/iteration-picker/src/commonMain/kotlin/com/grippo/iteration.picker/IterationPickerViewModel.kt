package com.grippo.iteration.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.IterationFocus
import com.grippo.core.state.trainings.IterationState
import kotlinx.collections.immutable.toPersistentList

public class IterationPickerViewModel(
    initial: IterationState,
    suggestions: List<IterationState>,
    number: Int,
    focus: IterationFocus
) : BaseViewModel<IterationPickerState, IterationPickerDirection, IterationPickerLoader>(
    IterationPickerState(
        value = initial,
        number = number,
        suggestions = suggestions.toPersistentList(),
        focus = focus,
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

    override fun onIterationClick(id: String) {
        update {
            val selected = it.suggestions.find { f -> f.id == id } ?: return@update it
            val iteration = it.value.copy(
                volume = selected.volume,
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