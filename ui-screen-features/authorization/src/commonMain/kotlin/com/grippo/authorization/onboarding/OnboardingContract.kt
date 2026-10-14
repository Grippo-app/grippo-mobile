package com.grippo.authorization.onboarding

import androidx.compose.runtime.Immutable

@Immutable
internal interface OnboardingContract {
    fun onAnalyticsConsentChange(granted: Boolean)
    fun onGetStarted()
    fun onBack()

    @Immutable
    companion object Empty : OnboardingContract {
        override fun onAnalyticsConsentChange(granted: Boolean) {}
        override fun onGetStarted() {}
        override fun onBack() {}
    }
}
