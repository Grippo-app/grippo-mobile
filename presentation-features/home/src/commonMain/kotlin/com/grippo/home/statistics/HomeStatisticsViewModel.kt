package com.grippo.home.statistics

import com.grippo.core.BaseViewModel

internal class HomeStatisticsViewModel :
    BaseViewModel<HomeStatisticsState, HomeStatisticsDirection, HomeStatisticsLoader>(
        HomeStatisticsState
    ),
    HomeStatisticsContract