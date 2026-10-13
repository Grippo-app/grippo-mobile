package com.grippo.profile.settings

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileSettingsContract {
    fun onAnalyticsConsentChange(granted: Boolean)
    fun onBack()
    fun onLogoutClick()
    fun onDeleteAccount()

    @Immutable
    companion object Empty : ProfileSettingsContract {
        override fun onAnalyticsConsentChange(granted: Boolean) {}
        override fun onBack() {}
        override fun onLogoutClick() {}
        override fun onDeleteAccount() {}
    }
}
