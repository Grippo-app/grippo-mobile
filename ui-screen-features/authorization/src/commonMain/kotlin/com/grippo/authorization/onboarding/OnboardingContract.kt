package com.grippo.authorization.onboarding

import androidx.compose.runtime.Immutable

@Immutable
internal interface OnboardingContract {
    fun onGetStarted()
    fun onBack()

    @Immutable
    companion object Empty : OnboardingContract {
        override fun onGetStarted() {}
        override fun onBack() {}
    }
}
