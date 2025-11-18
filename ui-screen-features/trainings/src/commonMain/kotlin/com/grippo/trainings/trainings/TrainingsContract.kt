package com.grippo.trainings.trainings

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate

@Immutable
internal interface TrainingsContract {
    fun onAddTraining()
    fun onDailyDigestViewStats()
    fun onExerciseClick(id: String)
    fun onOpenProfile()
    fun onTrainingMenuClick(id: String)
    fun onShiftDate(days: Int)
    fun onSelectPeriod(period: TrainingsTimelinePeriod)
    fun onOpenDaily(date: LocalDate)
    fun onBack()

    @Immutable
    companion object Empty : TrainingsContract {
        override fun onAddTraining() {}
        override fun onDailyDigestViewStats() {}
        override fun onExerciseClick(id: String) {}
        override fun onOpenProfile() {}
        override fun onTrainingMenuClick(id: String) {}
        override fun onShiftDate(days: Int) {}
        override fun onSelectPeriod(period: TrainingsTimelinePeriod) {}
        override fun onOpenDaily(date: LocalDate) {}
        override fun onBack() {}
    }
}
