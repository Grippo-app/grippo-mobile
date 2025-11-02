package com.grippo.authorization.registration.name

import androidx.compose.runtime.Immutable

@Immutable
internal interface NameContract {
    fun onNameChange(value: String)
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : NameContract {
        override fun onNameChange(value: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}