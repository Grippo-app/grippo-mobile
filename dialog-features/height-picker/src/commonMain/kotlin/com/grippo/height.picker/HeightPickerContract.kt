package com.grippo.height.picker

internal interface HeightPickerContract {

    fun dismiss()
    fun submit()

    companion object Empty : HeightPickerContract {
        override fun dismiss() {}
        override fun submit() {}
    }
}