package com.grippo.training.recording

internal interface TrainingRecordingContract {
    fun onAddExercise()
    fun onOpenFilters()
    fun onOpenExerciseExample(id: String)
    fun onAddIteration(exerciseId: String)
    fun onEditIteration(exerciseId: String, iterationId: String)
    fun onRemoveIteration(exerciseId: String, iterationId: String)
    fun onOpenStats()
    fun onOpenExercises()
    fun onSave()
    fun onBack()

    companion object Empty : TrainingRecordingContract {
        override fun onAddExercise() {}
        override fun onOpenFilters() {}
        override fun onOpenExerciseExample(id: String) {}
        override fun onAddIteration(exerciseId: String) {}
        override fun onEditIteration(exerciseId: String, iterationId: String) {}
        override fun onRemoveIteration(exerciseId: String, iterationId: String) {}
        override fun onOpenStats() {}
        override fun onOpenExercises() {}
        override fun onSave() {}
        override fun onBack() {}
    }
}