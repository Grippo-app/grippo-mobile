package com.grippo.training.recording

internal interface TrainingRecordingContract {
    fun onAddExercise()
    fun onOpenFilters()
    fun onOpenExerciseExample(id: String)
    fun onAddIteration(exerciseId: String)
    fun onEditIteration(exerciseId: String, iterationId: String)
    fun onRemoveIteration(exerciseId: String, iterationId: String)
    fun onSelectTab(tab: RecordingTab)
    fun onSave()
    fun onBack()

    companion object Empty : TrainingRecordingContract {
        override fun onAddExercise() {}
        override fun onOpenFilters() {}
        override fun onOpenExerciseExample(id: String) {}
        override fun onAddIteration(exerciseId: String) {}
        override fun onEditIteration(exerciseId: String, iterationId: String) {}
        override fun onRemoveIteration(exerciseId: String, iterationId: String) {}
        override fun onSelectTab(tab: RecordingTab) {}
        override fun onSave() {}
        override fun onBack() {}
    }
}