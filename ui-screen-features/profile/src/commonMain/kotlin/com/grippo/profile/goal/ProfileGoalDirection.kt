package com.grippo.profile.goal

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileGoalDirection : BaseDirection {
    data object Back : ProfileGoalDirection
}
