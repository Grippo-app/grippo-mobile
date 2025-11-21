package com.grippo.training.recording

import androidx.compose.runtime.Immutable

@Immutable
internal interface TrainingRecordingContract {
    fun onAddExercise()
    fun onEditExercise(id: String)
    fun onDeleteExercise(id: String)
    fun onSelectTab(tab: RecordingTab)
    fun onSave()
    fun onBack()

    @Immutable
    companion object Empty : TrainingRecordingContract {
        override fun onAddExercise() {}
        override fun onEditExercise(id: String) {}
        override fun onDeleteExercise(id: String) {}
        override fun onSelectTab(tab: RecordingTab) {}
        override fun onSave() {}
        override fun onBack() {}
    }
}