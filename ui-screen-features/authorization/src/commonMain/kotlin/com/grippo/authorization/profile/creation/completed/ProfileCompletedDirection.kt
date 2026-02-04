package com.grippo.authorization.profile.creation.completed

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileCompletedDirection : BaseDirection {
    data object Home : ProfileCompletedDirection
    data object Back : ProfileCompletedDirection
}