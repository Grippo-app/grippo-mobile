package com.grippo.home.statistics

import com.grippo.core.BaseViewModel

internal class StatisticsViewModel :
    BaseViewModel<StatisticsState, StatisticsDirection, StatisticsLoader>(StatisticsState),
    StatisticsContract