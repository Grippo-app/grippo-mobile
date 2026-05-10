package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseExampleContract {
    fun onDismiss()
    fun onAction()

    @Immutable
    companion object Empty : ExerciseExampleContract {
        override fun onDismiss() {}
        override fun onAction() {}
    }
}
