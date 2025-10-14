package com.grippo.profile

import com.grippo.core.foundation.models.BaseDirection

public sealed interface ProfileDirection : BaseDirection {
    public data object Back : ProfileDirection
}