package com.grippo.profile.body

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileBodyDirection : BaseDirection {
    data object Back : ProfileBodyDirection
}