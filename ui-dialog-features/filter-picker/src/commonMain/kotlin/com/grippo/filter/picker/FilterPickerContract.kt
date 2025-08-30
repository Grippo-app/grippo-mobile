package com.grippo.filter.picker

import com.grippo.state.filters.FilterValue

internal interface FilterPickerContract {
    fun onItemClick(value: FilterValue)
    fun onSubmitClick()
    fun onDismiss()

    companion object Empty : FilterPickerContract {
        override fun onItemClick(value: FilterValue) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}