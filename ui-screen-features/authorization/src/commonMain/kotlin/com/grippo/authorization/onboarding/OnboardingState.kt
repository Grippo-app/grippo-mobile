package com.grippo.authorization.onboarding

import androidx.compose.runtime.Immutable

@Immutable
internal data class OnboardingState(
    val analyticsConsent: Boolean? = null,
)
