package com.grippo.authorization.auth.process

internal interface AuthProcessContract {
    fun onClose()
    fun toRegistration()
    fun toHome()
    fun onBack()

    companion object Empty : AuthProcessContract {
        override fun onClose() {}
        override fun toRegistration() {}
        override fun toHome() {}
        override fun onBack() {}
    }
}