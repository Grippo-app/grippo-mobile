package com.grippo.profile

internal interface ProfileContract {
    fun onBack()

    companion object Empty : ProfileContract {
        override fun onBack() {}
    }
}