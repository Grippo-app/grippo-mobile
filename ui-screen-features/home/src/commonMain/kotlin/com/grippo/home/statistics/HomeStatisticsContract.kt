package com.grippo.home.statistics

internal interface HomeStatisticsContract {
    fun onSelectPeriod()
    fun onFiltersClick()
    fun onBack()

    companion object Empty : HomeStatisticsContract {
        override fun onSelectPeriod() {}
        override fun onFiltersClick() {}
        override fun onBack() {}
    }
}