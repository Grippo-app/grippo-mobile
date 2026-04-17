package com.grippo.profile.goal

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.PersonalizationKeyEnumState

@Immutable
internal interface ProfileGoalContract {
    fun onBack()
    fun onSave()
    fun onPrimaryGoalPickerClick()
    fun onSecondaryGoalPickerClick()
    fun onTargetDatePickerClick()
    fun onPersonalizationClick(item: PersonalizationKeyEnumState)

    @Immutable
    companion object Empty : ProfileGoalContract {
        override fun onBack() {}
        override fun onSave() {}
        override fun onPrimaryGoalPickerClick() {}
        override fun onSecondaryGoalPickerClick() {}
        override fun onTargetDatePickerClick() {}
        override fun onPersonalizationClick(item: PersonalizationKeyEnumState) {}
    }
}
