package com.grippo.height.picker

import androidx.compose.runtime.Immutable

@Immutable
internal interface HeightPickerContract {
    fun onSelectHeight(value: Int)
    fun onSubmitClick()
    fun onDismiss()

    @Immutable
    companion object Empty : HeightPickerContract {
        override fun onSelectHeight(value: Int) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}