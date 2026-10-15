package com.grippo.home

import com.grippo.core.foundation.models.BaseDirection

public sealed interface HomeRootDirection : BaseDirection {
    public data object Back : HomeRootDirection
    public data object Trainings : HomeRootDirection
    public data object Goal : HomeRootDirection
}
