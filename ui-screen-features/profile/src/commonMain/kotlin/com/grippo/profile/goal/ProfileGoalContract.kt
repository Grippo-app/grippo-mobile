package com.grippo.profile.goal

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileGoalContract {
    fun onBack()
    fun onSave()
    fun onPrimaryGoalPickerClick()
    fun onSecondaryGoalPickerClick()
    fun onTargetDatePickerClick()

    @Immutable
    companion object Empty : ProfileGoalContract {
        override fun onBack() {}
        override fun onSave() {}
        override fun onPrimaryGoalPickerClick() {}
        override fun onSecondaryGoalPickerClick() {}
        override fun onTargetDatePickerClick() {}
    }
}
