package com.grippo.training.recording

internal interface TrainingRecordingContract {
    fun onAddExercise()
    fun onEditExercise(id: String)
    fun onSelectTab(tab: RecordingTab)
    fun onSave()
    fun onBack()

    companion object Empty : TrainingRecordingContract {
        override fun onAddExercise() {}
        override fun onEditExercise(id: String) {}
        override fun onSelectTab(tab: RecordingTab) {}
        override fun onSave() {}
        override fun onBack() {}
    }
}