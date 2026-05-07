package com.grippo.training.recording

import androidx.compose.runtime.Immutable

@Immutable
internal interface TrainingRecordingContract {
    fun onAddExercise()
    fun onEditExercise(id: String)
    fun onDeleteExercise(id: String)
    fun onStartReorderExercises(fromId: String, toId: String)
    fun onEndReorderExercises()
    fun onSave()
    fun onBack()

    @Immutable
    companion object Empty : TrainingRecordingContract {
        override fun onAddExercise() {}
        override fun onEditExercise(id: String) {}
        override fun onDeleteExercise(id: String) {}
        override fun onStartReorderExercises(fromId: String, toId: String) {}
        override fun onEndReorderExercises() {}
        override fun onSave() {}
        override fun onBack() {}
    }
}