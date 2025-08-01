package com.grippo.authorization.registration.body

internal interface BodyContract {
    fun onWeightPickerClick()
    fun onHeightPickerClick()
    fun onNextClick()
    fun onBack()

    companion object Empty : BodyContract {
        override fun onWeightPickerClick() {}
        override fun onHeightPickerClick() {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}