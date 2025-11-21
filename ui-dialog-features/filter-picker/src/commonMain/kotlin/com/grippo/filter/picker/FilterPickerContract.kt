package com.grippo.filter.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.filters.FilterValue

@Immutable
internal interface FilterPickerContract {
    fun onItemClick(value: FilterValue)
    fun onSubmitClick()
    fun onReset()
    fun onDismiss()

    @Immutable
    companion object Empty : FilterPickerContract {
        override fun onItemClick(value: FilterValue) {}
        override fun onReset() {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}