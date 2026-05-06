package com.grippo.training.exercise

import androidx.compose.runtime.Immutable

@Immutable
internal interface TrainingExerciseContract {
    fun onAddIteration()
    fun onExampleClick()
    fun onEditVolume(id: String)
    fun onEditRepetition(id: String)
    fun onDeleteIteration(id: String)
    fun onSave()
    fun onBack()

    @Immutable
    companion object Empty : TrainingExerciseContract {
        override fun onExampleClick() {}
        override fun onAddIteration() {}
        override fun onEditVolume(id: String) {}
        override fun onEditRepetition(id: String) {}
        override fun onDeleteIteration(id: String) {}
        override fun onSave() {}
        override fun onBack() {}
    }
}
