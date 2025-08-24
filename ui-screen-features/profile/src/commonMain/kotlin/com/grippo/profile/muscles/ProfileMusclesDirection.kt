package com.grippo.profile.muscles

import com.grippo.core.models.BaseDirection

internal sealed interface ProfileMusclesDirection : BaseDirection {
    data object Back : ProfileMusclesDirection
}