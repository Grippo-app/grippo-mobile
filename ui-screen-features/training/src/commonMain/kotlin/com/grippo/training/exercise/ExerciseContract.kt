package com.grippo.training.exercise

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseContract {
    fun onAddIteration()
    fun onExampleClick()
    fun onEditVolume(id: String)
    fun onEditRepetition(id: String)
    fun onDeleteIteration(id: String)
    fun onRemoveIteration(id: String)
    fun onSave()
    fun onBack()

    @Immutable
    companion object Empty : ExerciseContract {
        override fun onExampleClick() {}
        override fun onAddIteration() {}
        override fun onEditVolume(id: String) {}
        override fun onEditRepetition(id: String) {}
        override fun onRemoveIteration(id: String) {}
        override fun onDeleteIteration(id: String) {}
        override fun onSave() {}
        override fun onBack() {}
    }
}
