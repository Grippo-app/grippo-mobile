package com.grippo.profile

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileContract {
    fun onBack()

    @Immutable
    companion object Empty : ProfileContract {
        override fun onBack() {}
    }
}