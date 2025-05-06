package com.grippo.height.picker

internal interface HeightPickerContract {

    fun select(value: Int)
    fun submit()

    companion object Empty : HeightPickerContract {
        override fun select(value: Int) {}
        override fun submit() {}
    }
}