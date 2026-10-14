package com.grippo.authorization.onboarding

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface OnboardingDirection : BaseDirection {
    data object Login : OnboardingDirection
}
