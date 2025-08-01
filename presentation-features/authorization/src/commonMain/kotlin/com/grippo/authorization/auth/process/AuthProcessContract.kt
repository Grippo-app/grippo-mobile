package com.grippo.authorization.auth.process

internal interface AuthProcessContract {
    fun onBack()

    companion object Empty : AuthProcessContract {
        override fun onBack() {}
    }
}