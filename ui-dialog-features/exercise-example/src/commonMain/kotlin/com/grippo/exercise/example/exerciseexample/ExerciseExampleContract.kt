package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseExampleContract {
    fun onDismiss()

    @Immutable
    companion object Empty : ExerciseExampleContract {
        override fun onDismiss() {}
    }
}