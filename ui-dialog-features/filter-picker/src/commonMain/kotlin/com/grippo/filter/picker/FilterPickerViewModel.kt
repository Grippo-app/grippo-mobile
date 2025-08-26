package com.grippo.filter.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.filters.FilterValue
import kotlinx.collections.immutable.toPersistentList

public class FilterPickerViewModel(
    initial: List<FilterValue>,
) : BaseViewModel<FilterPickerState, FilterPickerDirection, FilterPickerLoader>(
    FilterPickerState(
        list = initial.toPersistentList(),
    )
), FilterPickerContract {
    override fun onSubmitClick() {
        navigateTo(FilterPickerDirection.BackWithResult(state.value.list))
    }

    override fun onDismiss() {
        navigateTo(FilterPickerDirection.Back)
    }
}