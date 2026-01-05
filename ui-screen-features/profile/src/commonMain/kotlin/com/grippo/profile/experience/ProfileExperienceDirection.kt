package com.grippo.profile.experience

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileExperienceDirection : BaseDirection {
    data object Back : ProfileExperienceDirection
}
