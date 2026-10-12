package com.grippo.authorization

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.screen.api.AuthorizationEntryPoint

public sealed interface AuthDirection : BaseDirection {
    public data class AuthProcess(val entryPoint: AuthorizationEntryPoint) : AuthDirection
    public data object Back : AuthDirection
    public data object Home : AuthDirection
    public data object ProfileCreation : AuthDirection
}
