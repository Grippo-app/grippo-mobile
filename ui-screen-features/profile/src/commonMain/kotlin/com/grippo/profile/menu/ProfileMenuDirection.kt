package com.grippo.profile.menu

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu

internal sealed interface ProfileMenuDirection : BaseDirection {
    data class ProfileMenuSelected(val item: ProfileMenu) : ProfileMenuDirection
    data class SettingsMenuSelected(val item: SettingsMenu) : ProfileMenuDirection
}
