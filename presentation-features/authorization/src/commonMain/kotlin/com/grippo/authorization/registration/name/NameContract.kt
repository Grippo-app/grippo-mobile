package com.grippo.authorization.registration.name

internal interface NameContract {
    fun onNameChange(value: String)
    fun onNextClick()
    fun onBack()

    companion object Empty : NameContract {
        override fun onNameChange(value: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}