package com.grippo.home.trainings

internal interface HomeTrainingsContract {
    fun onExerciseClick(id: String)
    fun onSelectDate()
    fun onBack()

    companion object Empty : HomeTrainingsContract {
        override fun onExerciseClick(id: String) {}
        override fun onSelectDate() {}
        override fun onBack() {}
    }
}