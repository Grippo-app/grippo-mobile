package com.grippo.weight.picker

internal interface WeightPickerContract {
    fun onSelectWeight(value: Float)
    fun onSubmitClick()
    fun onDismiss()

    companion object Empty : WeightPickerContract {
        override fun onSelectWeight(value: Float) {}
        override fun onSubmitClick() {}
        override fun onDismiss() {}
    }
}