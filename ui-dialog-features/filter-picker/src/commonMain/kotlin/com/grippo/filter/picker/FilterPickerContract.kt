package com.grippo.filter.picker

import com.grippo.core.state.filters.FilterValue

internal interface FilterPickerContract {
    fun onItemClick(value: FilterValue)
    fun onSubmitClick()
    fun onReset()
    fun onDismiss()

    companion object Empty : FilterPickerContract {
        override fun onItemClick(value: FilterValue) {}
        override fun onReset() {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}