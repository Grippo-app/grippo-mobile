package com.grippo.dialog.profile

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu

public sealed interface ProfileDirection : BaseDirection {
    public data class BackWithProfileMenuResult(val item: ProfileMenu) : ProfileDirection
    public data class BackWithSettingsMenuResult(val item: SettingsMenu) : ProfileDirection
    public data object Back : ProfileDirection
}