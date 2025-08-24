package com.grippo.profile

import com.grippo.core.models.BaseDirection

public sealed interface ProfileDirection : BaseDirection {
    public data object Back : ProfileDirection
}