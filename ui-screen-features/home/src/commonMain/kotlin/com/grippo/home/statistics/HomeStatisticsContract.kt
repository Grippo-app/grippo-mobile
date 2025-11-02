package com.grippo.home.statistics

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeStatisticsContract {
    fun onSelectPeriod()
    fun onBack()

    @Immutable
    companion object Empty : HomeStatisticsContract {
        override fun onSelectPeriod() {}
        override fun onBack() {}
    }
}