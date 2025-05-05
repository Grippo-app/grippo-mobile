package com.grippo.weight.picker

internal interface WeightPickerContract {

    fun dismiss()

    companion object Empty : WeightPickerContract {
        override fun dismiss() {}
    }
}