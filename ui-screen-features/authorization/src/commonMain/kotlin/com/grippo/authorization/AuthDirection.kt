package com.grippo.authorization

import com.grippo.core.foundation.models.BaseDirection

public sealed interface AuthDirection : BaseDirection {
    public data object AuthProcess : AuthDirection
    public data object Back : AuthDirection
    public data object Home : AuthDirection
    public data object ProfileCreation : AuthDirection
}
