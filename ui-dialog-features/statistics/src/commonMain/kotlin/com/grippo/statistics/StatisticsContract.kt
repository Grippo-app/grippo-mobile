package com.grippo.statistics

import androidx.compose.runtime.Immutable

@Immutable
internal interface StatisticsContract {
    fun onBack()

    @Immutable
    companion object Empty : StatisticsContract {
        override fun onBack() {}
    }
}
