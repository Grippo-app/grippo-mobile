package com.grippo.profile

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.screen.api.ProfileRouter

public sealed interface ProfileDirection : BaseDirection {
    public data class Open(val router: ProfileRouter) : ProfileDirection
    public data object Debug : ProfileDirection
    public data object Back : ProfileDirection
}
