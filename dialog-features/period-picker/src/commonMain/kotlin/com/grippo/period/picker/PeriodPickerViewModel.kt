package com.grippo.period.picker

import com.grippo.core.BaseViewModel
import com.grippo.design.resources.Res
import com.grippo.design.resources.StringProvider
import com.grippo.design.resources.from
import com.grippo.design.resources.to
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public class PeriodPickerViewModel(
    initial: PeriodState,
    available: List<PeriodState>,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        value = initial,
        list = available.toPersistentList()
    )
), PeriodPickerContract {

    override fun onSelectClick(value: PeriodState) {
        update { it.copy(value = value, list = it.list.synchronize(value)) }
    }

    override fun onFromClick() {
        val custom = (state.value.value as? PeriodState.CUSTOM) ?: return

        safeLaunch {
            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.from),
                initial = custom.range.from,
                limitations = custom.limitations.copy(to = custom.range.to),
                onResult = { value ->
                    val range = custom.range.copy(from = value)
                    val newValue = custom.copy(range = range)
                    update { it.copy(value = newValue, list = it.list.synchronize(newValue)) }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onToClick() {
        val custom = (state.value.value as? PeriodState.CUSTOM) ?: return

        safeLaunch {
            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.to),
                initial = custom.range.to,
                limitations = custom.limitations.copy(from = custom.range.from),
                onResult = { value ->
                    val range = custom.range.copy(to = value)
                    val newValue = custom.copy(range = range)
                    update { it.copy(value = newValue, list = it.list.synchronize(newValue)) }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onSubmitClick() {
        navigateTo(PeriodPickerDirection.BackWithResult(state.value.value))
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