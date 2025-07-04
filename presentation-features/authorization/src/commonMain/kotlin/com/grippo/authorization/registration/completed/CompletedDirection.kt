package com.grippo.authorization.registration.completed

import com.grippo.core.models.BaseDirection

internal sealed interface CompletedDirection : BaseDirection {
    data object Home : CompletedDirection
    data object Back : CompletedDirection
}