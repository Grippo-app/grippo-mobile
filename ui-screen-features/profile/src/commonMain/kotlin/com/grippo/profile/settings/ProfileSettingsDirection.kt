package com.grippo.profile.settings

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileSettingsDirection : BaseDirection {
    data object Back : ProfileSettingsDirection
}
