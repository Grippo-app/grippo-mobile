package com.grippo.trainings.trainings

import androidx.compose.runtime.Immutable

@Immutable
internal interface TrainingsContract {
    fun onAddTraining()
    fun onDailyDigestViewStats()
    fun onExerciseClick(id: String)
    fun onOpenProfile()
    fun onTrainingMenuClick(id: String)
    fun onSelectDate()
    fun onShiftDate(days: Int)
    fun onSelectPeriod(period: TrainingsTimelinePeriod)
    fun onBack()

    @Immutable
    companion object Empty : TrainingsContract {
        override fun onAddTraining() {}
        override fun onDailyDigestViewStats() {}
        override fun onExerciseClick(id: String) {}
        override fun onOpenProfile() {}
        override fun onTrainingMenuClick(id: String) {}
        override fun onSelectDate() {}
        override fun onShiftDate(days: Int) {}
        override fun onSelectPeriod(period: TrainingsTimelinePeriod) {}
        override fun onBack() {}
    }
}
