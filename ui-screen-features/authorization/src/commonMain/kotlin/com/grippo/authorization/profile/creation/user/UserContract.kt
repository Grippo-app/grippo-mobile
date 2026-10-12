package com.grippo.authorization.profile.creation.user

import androidx.compose.runtime.Immutable

@Immutable
internal interface UserContract {
    fun onNameChange(value: String)
    fun onNameFocused()
    fun onNameCleared()
    fun onNameKeyboardDone()
    fun onWeightPickerClick()
    fun onHeightPickerClick()
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : UserContract {
        override fun onNameChange(value: String) {}
        override fun onNameFocused() {}
        override fun onNameCleared() {}
        override fun onNameKeyboardDone() {}
        override fun onWeightPickerClick() {}
        override fun onHeightPickerClick() {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}
