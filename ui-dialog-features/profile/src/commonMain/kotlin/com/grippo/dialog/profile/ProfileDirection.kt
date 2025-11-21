package com.grippo.dialog.profile

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.ProfileActivityMenu

public sealed interface ProfileDirection : BaseDirection {
    public data class BackWithResult(val item: ProfileActivityMenu) : ProfileDirection
    public data object Back : ProfileDirection
}