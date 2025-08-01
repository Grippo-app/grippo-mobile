package com.grippo.authorization

internal interface AuthContract {
    fun onBack()

    companion object Empty : AuthContract {
        override fun onBack() {}
    }
}