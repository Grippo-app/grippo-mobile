package com.grippo.profile.muscles

internal interface ProfileMusclesContract {

    fun select(id: String)
    fun apply()
    fun back()

    companion object Empty : ProfileMusclesContract {
        override fun select(id: String) {}
        override fun apply() {}
        override fun back() {}
    }
}