package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Immutable

@Immutable
internal interface CompletedContract {
    fun onCompleteClick()
    fun onBack()

    @Immutable
    companion object Empty : CompletedContract {
        override fun onCompleteClick() {}
        override fun onBack() {}
    }
}