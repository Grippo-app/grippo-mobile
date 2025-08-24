package com.grippo.home.statistics

internal interface HomeStatisticsContract {
    fun onSelectPeriod()
    fun onBack()

    companion object Empty : HomeStatisticsContract {
        override fun onSelectPeriod() {}
        override fun onBack() {}
    }
}