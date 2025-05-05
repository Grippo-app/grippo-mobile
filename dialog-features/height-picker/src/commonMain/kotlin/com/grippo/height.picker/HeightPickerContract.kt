package com.grippo.height.picker

internal interface HeightPickerContract {

    fun dismiss()

    companion object Empty : HeightPickerContract {
        override fun dismiss() {}
    }
}