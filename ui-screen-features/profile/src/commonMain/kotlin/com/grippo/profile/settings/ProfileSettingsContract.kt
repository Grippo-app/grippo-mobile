package com.grippo.profile.settings

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileSettingsContract {
    fun onBack()
    fun onDeleteAccount()

    @Immutable
    companion object Empty : ProfileSettingsContract {
        override fun onBack() {}
        override fun onDeleteAccount() {}
    }
}
