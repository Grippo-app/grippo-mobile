package com.grippo.authorization.auth.process

import com.grippo.core.models.BaseDirection

internal sealed interface AuthProcessDirection : BaseDirection {
    data object Close : AuthProcessDirection
    data object ToRegistration : AuthProcessDirection
    data object ToHome : AuthProcessDirection
    data object Back : AuthProcessDirection
}