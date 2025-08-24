package com.grippo.home

internal interface BottomNavigationContract {

    fun selectTab(origin: Int)
    fun onBack()
    fun toExcludedMuscles()
    fun toMissingEquipment()
    fun toWeightHistory()
    fun toExerciseExamples()
    fun toDebug()
    fun toWorkout()
    fun toSystemSettings()

    companion object Empty : BottomNavigationContract {
        override fun selectTab(origin: Int) {}
        override fun onBack() {}
        override fun toExcludedMuscles() {}
        override fun toMissingEquipment() {}
        override fun toWeightHistory() {}
        override fun toExerciseExamples() {}
        override fun toDebug() {}
        override fun toWorkout() {}
        override fun toSystemSettings() {}
    }
}