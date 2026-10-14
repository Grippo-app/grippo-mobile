package com.grippo.authorization.onboarding

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface OnboardingLoader : BaseLoader {
    @Immutable
    data object Preparation : OnboardingLoader

    @Immutable
    data object AnalyticsConsent : OnboardingLoader
}
