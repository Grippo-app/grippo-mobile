package com.grippo.shared.root

public interface RootContract {
    public fun onClose()
    public fun toHome()
    public fun toProfile()
    public fun toDebug()
    public fun toSettings()
    public fun toWorkout()
    public fun toWeightHistory()
    public fun toMissingEquipment()
    public fun toExcludedMuscles()
    public fun toExerciseExamples()
    public fun toSystemSettings()
    public fun onBack()

    public companion object Empty : RootContract {
        override fun onClose() {}
        override fun toHome() {}
        override fun toProfile() {}
        override fun toDebug() {}
        override fun toSettings() {}
        override fun toExerciseExamples() {}
        override fun toWorkout() {}
        override fun toWeightHistory() {}
        override fun toMissingEquipment() {}
        override fun toExcludedMuscles() {}
        override fun toSystemSettings() {}
        override fun onBack() {}
    }
}