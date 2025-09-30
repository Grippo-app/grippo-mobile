package com.grippo.period.picker

import com.grippo.core.BaseViewModel
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.select_end_date
import com.grippo.design.resources.provider.select_start_date
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.DateFormatState
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

        if (state.value.list.none { it is PeriodState.Custom }) {
            navigateTo(PeriodPickerDirection.BackWithResult(state.value.value))
        }
    }

    override fun onFromClick() {
        val custom = (state.value.value as? PeriodState.Custom) ?: return

        val limits = custom.limitations.copy(to = custom.range.to)
        val data = DateFormatState.of(custom.range.from, limits)

        safeLaunch {
            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.select_start_date),
                initial = data,
                limitations = limits,
                onResult = { value ->
                    val date = value.value ?: return@DatePicker
                    val range = custom.range.copy(from = date)
                    val newValue = custom.copy(range = range)
                    update { it.copy(value = newValue, list = it.list.synchronize(newValue)) }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onToClick() {
        val custom = (state.value.value as? PeriodState.Custom) ?: return

        safeLaunch {
            val limits = custom.limitations.copy(from = custom.range.from)
            val data = DateFormatState.of(custom.range.to, limits)

            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.select_end_date),
                initial = data,
                limitations = limits,
                onResult = { value ->
                    val date = value.value ?: return@DatePicker
                    val range = custom.range.copy(to = date)
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