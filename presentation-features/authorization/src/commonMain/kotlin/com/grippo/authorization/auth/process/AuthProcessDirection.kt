package com.grippo.authorization.auth.process

import com.grippo.core.models.BaseDirection

internal sealed interface AuthProcessDirection : BaseDirection {
    data object Back : AuthProcessDirection
}