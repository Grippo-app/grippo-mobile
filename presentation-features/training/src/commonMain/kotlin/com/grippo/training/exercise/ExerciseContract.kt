package com.grippo.training.exercise

internal interface ExerciseContract {
    fun onAddIteration()
    fun onEditIteration(id: String)
    fun onRemoveIteration(id: String)
    fun onSave()
    fun onBack()

    companion object Empty : ExerciseContract {
        override fun onAddIteration() {}
        override fun onEditIteration(id: String) {}
        override fun onRemoveIteration(id: String) {}
        override fun onSave() {}
        override fun onBack() {}
    }
}
