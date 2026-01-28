package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.PerformanceMetricTypeState

@Immutable
internal interface HomeContract {
    fun onOpenProfile()
    fun onOpenTrainingProfile()
    fun onStartTraining()
    fun onResumeTraining()
    fun onOpenTrainings()
    fun onOpenExample(id: String)
    fun onOpenMuscleLoading()
    fun onOpenTrainingStreak()
    fun onOpenPeriodPicker()
    fun onPerformanceMetricClick(type: PerformanceMetricTypeState)
    fun onOpenDigest()
    fun onBack()

    @Immutable
    companion object Empty : HomeContract {
        override fun onOpenProfile() {}
        override fun onOpenTrainingProfile() {}
        override fun onStartTraining() {}
        override fun onResumeTraining() {}
        override fun onOpenTrainings() {}
        override fun onOpenMuscleLoading() {}
        override fun onOpenTrainingStreak() {}
        override fun onOpenPeriodPicker() {}
        override fun onPerformanceMetricClick(type: PerformanceMetricTypeState) {}
        override fun onOpenExample(id: String) {}
        override fun onOpenDigest() {}
        override fun onBack() {}
    }
}
