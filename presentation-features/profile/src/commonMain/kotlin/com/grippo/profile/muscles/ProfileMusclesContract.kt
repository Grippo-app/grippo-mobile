package com.grippo.profile.muscles

internal interface ProfileMusclesContract {

    fun select(id: String)
    fun next()

    companion object Empty : ProfileMusclesContract {
        override fun select(id: String) {}
        override fun next() {}
    }
}