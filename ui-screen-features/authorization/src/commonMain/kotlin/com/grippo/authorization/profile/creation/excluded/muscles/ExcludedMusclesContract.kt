package com.grippo.authorization.profile.creation.excluded.muscles

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExcludedMusclesContract {
    fun onGroupClick(id: String)
    fun onSelect(id: String)
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : ExcludedMusclesContract {
        override fun onGroupClick(id: String) {}
        override fun onSelect(id: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}
