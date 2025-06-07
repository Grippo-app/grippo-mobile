package com.grippo.authorization.auth.process

internal interface AuthProcessContract {
    fun back()

    companion object Empty : AuthProcessContract {
        override fun back() {}
    }
}