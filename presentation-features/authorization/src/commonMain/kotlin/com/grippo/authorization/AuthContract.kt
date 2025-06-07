package com.grippo.authorization

internal interface AuthContract {
    fun back()

    companion object Empty : AuthContract {
        override fun back() {}
    }
}