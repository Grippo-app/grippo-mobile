package com.grippo.authorization.profile.creation.body

import androidx.compose.runtime.Immutable

@Immutable
internal interface BodyContract {
    fun onWeightPickerClick()
    fun onHeightPickerClick()
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : BodyContract {
        override fun onWeightPickerClick() {}
        override fun onHeightPickerClick() {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}