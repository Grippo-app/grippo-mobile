package com.grippo.filter.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.filters.FilterValueState
import kotlinx.collections.immutable.toPersistentList

public class FilterPickerViewModel(
    initial: List<FilterValueState>,
) : BaseViewModel<FilterPickerState, FilterPickerDirection, FilterPickerLoader>(
    FilterPickerState(
        list = initial.toPersistentList(),
    )
), FilterPickerContract {

    override fun onItemClick(value: FilterValueState) {
        update { state ->
            val list = state.list.map { item ->
                when (value) {
                    is FilterValueState.WeightType -> if (item is FilterValueState.WeightType) value else item
                    is FilterValueState.ForceType -> if (item is FilterValueState.ForceType) value else item
                    is FilterValueState.Category -> if (item is FilterValueState.Category) value else item
                }
            }

            state.copy(list = list.toPersistentList())
        }
    }

    override fun onReset() {
        update { state ->
            val list = state.list.map { item ->
                when (item) {
                    is FilterValueState.WeightType -> item.copy(value = null)
                    is FilterValueState.ForceType -> item.copy(value = null)
                    is FilterValueState.Category -> item.copy(value = null)
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