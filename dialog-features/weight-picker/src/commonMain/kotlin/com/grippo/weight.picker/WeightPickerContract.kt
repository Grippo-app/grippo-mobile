package com.grippo.weight.picker

internal interface WeightPickerContract {
    fun select(value: Float)
    fun submit()
    fun dismiss()

    companion object Empty : WeightPickerContract {
        override fun select(value: Float) {}
        override fun submit() {}
        override fun dismiss() {}
    }
}