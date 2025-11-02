package com.grippo.profile.muscles

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileMusclesContract {
    fun onSelect(id: String)
    fun onApply()
    fun onBack()

    @Immutable
    companion object Empty : ProfileMusclesContract {
        override fun onSelect(id: String) {}
        override fun onApply() {}
        override fun onBack() {}
    }
}