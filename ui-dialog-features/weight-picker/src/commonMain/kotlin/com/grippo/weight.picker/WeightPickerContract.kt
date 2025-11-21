package com.grippo.weight.picker

import androidx.compose.runtime.Immutable

@Immutable
internal interface WeightPickerContract {
    fun onSelectWeight(value: Float)
    fun onSubmitClick()
    fun onDismiss()

    @Immutable
    companion object Empty : WeightPickerContract {
        override fun onSelectWeight(value: Float) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}