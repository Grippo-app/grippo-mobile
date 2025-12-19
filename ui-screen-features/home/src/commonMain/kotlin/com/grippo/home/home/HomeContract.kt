package com.grippo.home.home

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeContract {
    fun onOpenProfile()
    fun onBack()

    @Immutable
    companion object Empty : HomeContract {
        override fun onOpenProfile() {}
        override fun onBack() {}
    }
}
