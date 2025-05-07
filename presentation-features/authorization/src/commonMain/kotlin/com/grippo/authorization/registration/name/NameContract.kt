package com.grippo.authorization.registration.name

internal interface NameContract {
    fun setName(value: String)
    fun next()

    companion object Empty : NameContract {
        override fun setName(value: String) {}
        override fun next() {}
    }
}