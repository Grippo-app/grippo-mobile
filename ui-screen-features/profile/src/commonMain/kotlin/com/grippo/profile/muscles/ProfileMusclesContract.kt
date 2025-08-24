package com.grippo.profile.muscles

internal interface ProfileMusclesContract {

    fun onSelect(id: String)
    fun onApply()
    fun onBack()

    companion object Empty : ProfileMusclesContract {
        override fun onSelect(id: String) {}
        override fun onApply() {}
        override fun onBack() {}
    }
}