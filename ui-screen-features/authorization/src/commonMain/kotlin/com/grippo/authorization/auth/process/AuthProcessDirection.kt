package com.grippo.authorization.auth.process

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface AuthProcessDirection : BaseDirection {
    data object Close : AuthProcessDirection
    data object Registration : AuthProcessDirection
    data object Home : AuthProcessDirection
    data object ProfileCreation : AuthProcessDirection
    data object Back : AuthProcessDirection
}
