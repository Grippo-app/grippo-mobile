package com.grippo.trainings.trainings

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate

@Immutable
internal interface TrainingsContract {
    fun onAddTraining()
    fun onDailyDigestViewStats()
    fun onExerciseClick(id: String)
    fun onTrainingMenuClick(id: String)
    fun onSelectPeriod(period: TrainingsTimelinePeriod)
    fun onOpenDateSelector()
    fun onSelectNextDate()
    fun onSelectPreviousDate()
    fun onOpenDaily(date: LocalDate)
    fun onBack()

    @Immutable
    companion object Empty : TrainingsContract {
        override fun onAddTraining() {}
        override fun onDailyDigestViewStats() {}
        override fun onExerciseClick(id: String) {}
        override fun onTrainingMenuClick(id: String) {}
        override fun onSelectPeriod(period: TrainingsTimelinePeriod) {}
        override fun onOpenDateSelector() {}
        override fun onSelectNextDate() {}
        override fun onSelectPreviousDate() {}
        override fun onOpenDaily(date: LocalDate) {}
        override fun onBack() {}
    }
}
