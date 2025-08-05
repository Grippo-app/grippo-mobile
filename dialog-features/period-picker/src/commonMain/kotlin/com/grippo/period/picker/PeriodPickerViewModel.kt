package com.grippo.period.picker

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.ImmutableList
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
        update { it.copy(initial = value, list = it.list.synchronize(value)) }
    }

    override fun onFromClick() {
        val custom = (state.value.initial as? PeriodState.CUSTOM) ?: return

        val dialog = DialogConfig.DatePicker(
            initial = custom.range.from,
            limitations = custom.limitations.copy(to = custom.range.to),
            onResult = { value ->
                val range = custom.range.copy(from = value)
                val newValue = custom.copy(range = range)
                update { it.copy(initial = newValue, list = it.list.synchronize(newValue)) }
            }
        )

        dialogController.show(dialog)
    }

    override fun onToClick() {
        val custom = (state.value.initial as? PeriodState.CUSTOM) ?: return

        val dialog = DialogConfig.DatePicker(
            initial = custom.range.to,
            limitations = custom.limitations.copy(from = custom.range.from),
            onResult = { value ->
                val range = custom.range.copy(to = value)
                val newValue = custom.copy(range = range)
                update { it.copy(initial = newValue, list = it.list.synchronize(newValue)) }
            }
        )

        dialogController.show(dialog)
    }

    override fun onSubmitClick() {
        navigateTo(PeriodPickerDirection.BackWithResult(state.value.initial))
    }

    override fun onDismiss() {
        navigateTo(PeriodPickerDirection.Back)
    }

    private fun List<PeriodState>.synchronize(value: PeriodState): ImmutableList<PeriodState> {
        return map { item ->
            if (item::class == value::class) value else item
        }.toPersistentList()
    }
}