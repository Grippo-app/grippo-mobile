package com.grippo.home.trainings

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeTrainingsContract {
    fun onAddTraining()
    fun onExerciseClick(id: String)
    fun onOpenProfile()
    fun onTrainingMenuClick(id: String)
    fun onSelectDate()
    fun onBack()

    @Immutable
    companion object Empty : HomeTrainingsContract {
        override fun onAddTraining() {}
        override fun onExerciseClick(id: String) {}
        override fun onOpenProfile() {}
        override fun onTrainingMenuClick(id: String) {}
        override fun onSelectDate() {}
        override fun onBack() {}
    }
}