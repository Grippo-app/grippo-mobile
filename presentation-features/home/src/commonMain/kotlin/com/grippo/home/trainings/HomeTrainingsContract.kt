package com.grippo.home.trainings

internal interface HomeTrainingsContract {

    fun openExerciseExample(id: String)
    fun selectDate()
    fun back()

    companion object Empty : HomeTrainingsContract {
        override fun openExerciseExample(id: String) {}
        override fun selectDate() {}
        override fun back() {}
    }
}