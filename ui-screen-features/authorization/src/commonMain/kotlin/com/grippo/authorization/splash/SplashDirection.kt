package com.grippo.authorization.splash

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.screen.api.AuthorizationEntryPoint

internal sealed interface SplashDirection : BaseDirection {
    data class AuthProcess(val entryPoint: AuthorizationEntryPoint) : SplashDirection
    data object Home : SplashDirection
    data object CreateProfile : SplashDirection
    data object Back : SplashDirection
}
