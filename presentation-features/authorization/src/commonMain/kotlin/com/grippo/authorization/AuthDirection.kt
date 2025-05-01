package com.grippo.authorization

import com.grippo.core.models.BaseDirection

internal sealed interface AuthDirection : BaseDirection {
    data object AuthProcess : AuthDirection
}