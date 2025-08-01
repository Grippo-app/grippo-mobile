package com.grippo.authorization.registration.completed

internal interface CompletedContract {
    fun onCompleteClick()
    fun onBack()

    companion object Empty : CompletedContract {
        override fun onCompleteClick() {}
        override fun onBack() {}
    }
}