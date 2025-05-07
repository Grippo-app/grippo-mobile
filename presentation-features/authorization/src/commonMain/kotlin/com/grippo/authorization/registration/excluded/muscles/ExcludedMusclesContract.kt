package com.grippo.authorization.registration.excluded.muscles

internal interface ExcludedMusclesContract {

    fun next()

    companion object Empty : ExcludedMusclesContract {
        override fun next() {}
    }
}