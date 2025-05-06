package com.grippo.weight.picker

internal interface WeightPickerContract {

    fun dismiss()
    fun submit()

    companion object Empty : WeightPickerContract {
        override fun dismiss() {}
        override fun submit() {}
    }
}