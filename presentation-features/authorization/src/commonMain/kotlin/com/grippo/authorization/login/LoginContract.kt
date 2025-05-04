package com.grippo.authorization.login

internal interface LoginContract {

    fun setEmail(value: String)
    fun setPassword(value: String)
    fun login()
    fun register()

    companion object Empty : LoginContract {
        override fun setEmail(value: String) {}
        override fun setPassword(value: String) {}
        override fun login() {}
        override fun register() {}
    }
}