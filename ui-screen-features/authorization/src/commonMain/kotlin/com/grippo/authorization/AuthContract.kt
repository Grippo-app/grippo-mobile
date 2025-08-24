package com.grippo.authorization

internal interface AuthContract {
    fun onBack()
    fun toAuthProcess()
    fun toHome()

    companion object Empty : AuthContract {
        override fun onBack() {}
        override fun toAuthProcess() {}
        override fun toHome() {}
    }
}