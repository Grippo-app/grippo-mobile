package com.grippo.weight.picker

internal interface WeightPickerContract {
    fun select(value: Float)
    fun submit()

    companion object Empty : WeightPickerContract {
        override fun select(value: Float) {}
        override fun submit() {}
    }
}