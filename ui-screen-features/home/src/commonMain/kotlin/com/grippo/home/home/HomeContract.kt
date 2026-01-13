package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.PerformanceMetricTypeState

@Immutable
internal interface HomeContract {
    fun onOpenProfile()
    fun onStartTraining()
    fun onOpenTrainings()
    fun onOpenExample(id: String)
    fun onOpenMuscleLoading()
    fun onOpenTrainingStreak()
    fun onPerformanceMetricClick(type: PerformanceMetricTypeState)
    fun onOpenWeeklyDigest()
    fun onOpenMonthlyDigest()
    fun onBack()

    @Immutable
    companion object Empty : HomeContract {
        override fun onOpenProfile() {}
        override fun onStartTraining() {}
        override fun onOpenTrainings() {}
        override fun onOpenMuscleLoading() {}
        override fun onOpenTrainingStreak() {}
        override fun onPerformanceMetricClick(type: PerformanceMetricTypeState) {}
        override fun onOpenExample(id: String) {}
        override fun onOpenWeeklyDigest() {}
        override fun onOpenMonthlyDigest() {}
        override fun onBack() {}
    }
}
