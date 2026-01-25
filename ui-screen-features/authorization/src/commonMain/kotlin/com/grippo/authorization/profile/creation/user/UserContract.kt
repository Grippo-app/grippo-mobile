package com.grippo.authorization.profile.creation.user

import androidx.compose.runtime.Immutable

@Immutable
internal interface UserContract {
    fun onNameChange(value: String)
    fun onWeightPickerClick()
    fun onHeightPickerClick()
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : UserContract {
        override fun onNameChange(value: String) {}
        override fun onWeightPickerClick() {}
        override fun onHeightPickerClick() {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}