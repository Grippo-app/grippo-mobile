package com.grippo.profile

internal interface ProfileContract {
    fun back()

    companion object Empty : ProfileContract {
        override fun back() {}
    }
}