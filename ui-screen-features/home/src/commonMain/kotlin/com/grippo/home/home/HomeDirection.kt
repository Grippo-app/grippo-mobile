package com.grippo.home.home

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface HomeDirection : BaseDirection {
    data object Back : HomeDirection
    data object Trainings : HomeDirection
    data object Goal : HomeDirection
}
