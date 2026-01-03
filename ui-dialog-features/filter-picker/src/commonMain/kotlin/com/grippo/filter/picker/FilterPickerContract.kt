package com.grippo.filter.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.filters.FilterValueState

@Immutable
internal interface FilterPickerContract {
    fun onItemClick(value: FilterValueState)
    fun onSubmitClick()
    fun onReset()
    fun onDismiss()

    @Immutable
    companion object Empty : FilterPickerContract {
        override fun onItemClick(value: FilterValueState) {}
        override fun onReset() {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}