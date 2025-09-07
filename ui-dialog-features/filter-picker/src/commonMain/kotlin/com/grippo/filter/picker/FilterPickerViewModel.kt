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

    override fun onItemClick(value: FilterValue) {
        update { state ->
            val list = state.list.map { item ->
                when (value) {
                    is FilterValue.WeightType -> if (item is FilterValue.WeightType) value else item
                    is FilterValue.ForceType -> if (item is FilterValue.ForceType) value else item
                    is FilterValue.Category -> if (item is FilterValue.Category) value else item
                    is FilterValue.Experience -> if (item is FilterValue.Experience) value else item
                }
            }

            state.copy(list = list.toPersistentList())
        }
    }

    override fun onReset() {
        update { state ->
            val list = state.list.map { item ->
                when (item) {
                    is FilterValue.WeightType -> item.copy(value = null)
                    is FilterValue.ForceType -> item.copy(value = null)
                    is FilterValue.Category -> item.copy(value = null)
                    is FilterValue.Experience -> item.copy(value = null)
                }
            }

            state.copy(list = list.toPersistentList())
        }

        navigateTo(FilterPickerDirection.BackWithResult(state.value.list))
    }

    override fun onSubmitClick() {
        navigateTo(FilterPickerDirection.BackWithResult(state.value.list))
    }

    override fun onDismiss() {
        navigateTo(FilterPickerDirection.Back)
    }
}