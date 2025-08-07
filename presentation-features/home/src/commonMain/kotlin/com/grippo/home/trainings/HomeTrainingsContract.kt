package com.grippo.home.trainings

internal interface HomeTrainingsContract {

    fun openExercise(id: String)
    fun selectDate()
    fun back()

    companion object Empty : HomeTrainingsContract {
        override fun openExercise(id: String) {}
        override fun selectDate() {}
        override fun back() {}
    }
}