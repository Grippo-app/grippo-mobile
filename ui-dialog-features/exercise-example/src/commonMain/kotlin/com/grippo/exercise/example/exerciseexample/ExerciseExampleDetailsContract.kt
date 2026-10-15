package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseExampleDetailsContract {
    fun onDismiss()
    fun onTabSelected(value: ExerciseExampleDetailsTabState)
    fun onAction()

    @Immutable
    companion object Empty : ExerciseExampleDetailsContract {
        override fun onDismiss() {}
        override fun onAction() {}
        override fun onTabSelected(value: ExerciseExampleDetailsTabState) {}
    }
}
