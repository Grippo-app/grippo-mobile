package com.grippo.home.statistics

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface HomeStatisticsDirection : BaseDirection {
    data object Back : HomeStatisticsDirection
}