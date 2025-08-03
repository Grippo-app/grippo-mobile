package com.grippo.height.picker

internal interface HeightPickerContract {
    fun onSelectHeight(value: Int)
    fun onSubmitClick()
    fun onDismiss()

    companion object Empty : HeightPickerContract {
        override fun onSelectHeight(value: Int) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}