package com.grippo.home.trainings

internal interface HomeTrainingsContract {

    fun openExerciseExample(id: String)

    companion object Empty : HomeTrainingsContract {
        override fun openExerciseExample(id: String) {}
    }
}