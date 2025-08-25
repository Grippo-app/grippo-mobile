package com.grippo.filter.picker

internal interface FilterPickerContract {
    fun onSubmitClick()
    fun onDismiss()

    companion object Empty : FilterPickerContract {
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}