package com.grippo.profile.body

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileBodyContract {
    fun onWeightPickerClick()
    fun onHeightPickerClick()
    fun onApplyClick()
    fun onBack()

    @Immutable
    companion object Empty : ProfileBodyContract {
        override fun onWeightPickerClick() {}
        override fun onHeightPickerClick() {}
        override fun onApplyClick() {}
        override fun onBack() {}
    }
}