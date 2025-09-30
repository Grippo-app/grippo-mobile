package com.grippo.home.trainings

internal interface HomeTrainingsContract {
    fun onExerciseClick(id: String)
    fun onTrainingMenuClick(id: String)
    fun onSelectDate()
    fun onNextClick()
    fun onPreviousClick()
    fun onBack()

    companion object Empty : HomeTrainingsContract {
        override fun onExerciseClick(id: String) {}
        override fun onTrainingMenuClick(id: String) {}
        override fun onSelectDate() {}
        override fun onNextClick() {}
        override fun onPreviousClick() {}
        override fun onBack() {}
    }
}