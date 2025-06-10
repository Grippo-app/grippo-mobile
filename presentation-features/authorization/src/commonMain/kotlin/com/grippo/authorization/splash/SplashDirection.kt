package com.grippo.authorization.splash

import com.grippo.core.models.BaseDirection

internal sealed interface SplashDirection : BaseDirection {
    data object AuthProcess : SplashDirection
    data object Home : SplashDirection
    data object Back : SplashDirection
}