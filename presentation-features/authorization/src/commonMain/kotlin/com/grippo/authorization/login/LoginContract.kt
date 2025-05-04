package com.grippo.authorization.login

internal interface LoginContract {

    fun setEmail(value: String)
    fun setPassword(value: String)
    fun login(value: String)

    companion object Empty : LoginContract {
        override fun setEmail(value: String) {}
        override fun setPassword(value: String) {}
        override fun login(value: String) {}
    }
}