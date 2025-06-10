package com.grippo.home.statistics

internal interface HomeStatisticsContract {
    fun back()

    companion object Empty : HomeStatisticsContract {
        override fun back() {}
    }
}