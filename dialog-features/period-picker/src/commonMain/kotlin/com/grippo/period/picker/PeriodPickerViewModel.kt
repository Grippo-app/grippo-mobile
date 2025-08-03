package com.grippo.period.picker

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.toPersistentList

public class PeriodPickerViewModel(
    initial: PeriodState,
    available: List<PeriodState>,
    private val dialogController: DialogController
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        initial = initial,
        list = available.toPersistentList()
    )
), PeriodPickerContract {

    override fun onSelectClick(value: PeriodState) {
        update { it.copy(initial = value) }
    }

    override fun onFromClick() {
        val custom = (state.value.initial as? PeriodState.CUSTOM) ?: return

        val dialog = DialogConfig.DatePicker(
            initial = custom.range.from,
            onResult = { value ->
                println("📅 onFromClick → received date: $value")
                val range = custom.range.copy(from = value)
                update { it.copy(initial = custom.copy(range = range)) }
            }
        )

        dialogController.show(dialog)
    }

    override fun onToClick() {
        val custom = (state.value.initial as? PeriodState.CUSTOM) ?: return

        val dialog = DialogConfig.DatePicker(
            initial = custom.range.to,
            onResult = { value ->
                println("📅 onToClick → received date: $value")
                val range = custom.range.copy(to = value)
                update { it.copy(initial = custom.copy(range = range)) }
            }
        )

        dialogController.show(dialog)
    }

    override fun submit() {
        navigateTo(PeriodPickerDirection.BackWithResult(state.value.initial))
    }

    override fun dismiss() {
        navigateTo(PeriodPickerDirection.Back)
    }
}