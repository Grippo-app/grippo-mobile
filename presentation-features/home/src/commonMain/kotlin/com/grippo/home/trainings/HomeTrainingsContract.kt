package com.grippo.home.trainings

internal interface HomeTrainingsContract {

    fun openExerciseExample(id: String)
    fun back()

    companion object Empty : HomeTrainingsContract {
        override fun openExerciseExample(id: String) {}
        override fun back() {}
    }
}