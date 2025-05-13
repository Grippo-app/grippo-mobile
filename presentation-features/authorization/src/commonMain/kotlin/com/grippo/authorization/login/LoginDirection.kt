package com.grippo.authorization.login

import com.grippo.core.models.BaseDirection

internal sealed interface LoginDirection : BaseDirection {
    data object Registration : LoginDirection
    data object Home : LoginDirection
}