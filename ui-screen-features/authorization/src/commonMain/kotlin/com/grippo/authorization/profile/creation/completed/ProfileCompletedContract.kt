package com.grippo.authorization.profile.creation.completed

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileCompletedContract {
    fun onCompleteClick()
    fun onBack()

    @Immutable
    companion object Empty : ProfileCompletedContract {
        override fun onCompleteClick() {}
        override fun onBack() {}
    }
}