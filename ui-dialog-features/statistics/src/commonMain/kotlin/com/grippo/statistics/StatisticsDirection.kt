package com.grippo.statistics

import com.grippo.core.foundation.models.BaseDirection

public sealed interface StatisticsDirection : BaseDirection {
    public data object Back : StatisticsDirection
}
