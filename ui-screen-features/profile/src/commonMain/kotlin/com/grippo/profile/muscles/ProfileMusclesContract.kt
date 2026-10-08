package com.grippo.profile.muscles

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileMusclesContract {
    fun onGroupClick(id: String)
    fun onSelect(id: String)
    fun onApply()
    fun onBack()

    @Immutable
    companion object Empty : ProfileMusclesContract {
        override fun onGroupClick(id: String) {}
        override fun onSelect(id: String) {}
        override fun onApply() {}
        override fun onBack() {}
    }
}
