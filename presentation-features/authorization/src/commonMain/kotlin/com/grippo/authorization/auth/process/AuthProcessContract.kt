package com.grippo.authorization.auth.process

internal interface AuthProcessContract {
    fun close()
    fun toRegistration()
    fun toHome()
    fun onBack()

    companion object Empty : AuthProcessContract {
        override fun close() {}
        override fun toRegistration() {}
        override fun toHome() {}
        override fun onBack() {}
    }
}