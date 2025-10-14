package com.grippo.profile.muscles

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileMusclesDirection : BaseDirection {
    data object Back : ProfileMusclesDirection
}