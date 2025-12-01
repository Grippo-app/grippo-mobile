package com.grippo.authorization.profile.creation.completed

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface CompletedDirection : BaseDirection {
    data object Home : CompletedDirection
    data object Back : CompletedDirection
}