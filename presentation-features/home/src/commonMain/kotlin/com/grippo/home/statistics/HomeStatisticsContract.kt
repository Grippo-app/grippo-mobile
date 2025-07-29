package com.grippo.home.statistics

internal interface HomeStatisticsContract {
    fun selectPeriod()
    fun back()

    companion object Empty : HomeStatisticsContract {
        override fun selectPeriod() {}
        override fun back() {}
    }
}