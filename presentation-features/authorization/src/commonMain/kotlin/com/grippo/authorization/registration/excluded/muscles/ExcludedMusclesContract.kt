package com.grippo.authorization.registration.excluded.muscles

internal interface ExcludedMusclesContract {
    fun select(id: String)
    fun next()
    fun back()

    companion object Empty : ExcludedMusclesContract {
        override fun select(id: String) {}
        override fun next() {}
        override fun back() {}
    }
}