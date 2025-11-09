package com.grippo.trainings

import androidx.compose.runtime.Immutable

@Immutable
internal interface TrainingsContract {
    fun onAddTraining()
    fun onExerciseClick(id: String)
    fun onOpenProfile()
    fun onTrainingMenuClick(id: String)
    fun onSelectDate()
    fun onBack()

    @Immutable
    companion object Empty : TrainingsContract {
        override fun onAddTraining() {}
        override fun onExerciseClick(id: String) {}
        override fun onOpenProfile() {}
        override fun onTrainingMenuClick(id: String) {}
        override fun onSelectDate() {}
        override fun onBack() {}
    }
}