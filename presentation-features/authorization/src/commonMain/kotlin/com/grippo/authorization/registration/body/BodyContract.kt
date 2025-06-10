package com.grippo.authorization.registration.body

internal interface BodyContract {
    fun openWeightPicker()
    fun openHeightPicker()
    fun next()
    fun back()

    companion object Empty : BodyContract {
        override fun openWeightPicker() {}
        override fun openHeightPicker() {}
        override fun next() {}
        override fun back() {}
    }
}