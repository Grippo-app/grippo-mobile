package com.grippo.home.trainings

internal interface TrainingsContract {

    fun openExerciseExample(id: String)

    companion object Empty : TrainingsContract {
        override fun openExerciseExample(id: String) {}
    }
}