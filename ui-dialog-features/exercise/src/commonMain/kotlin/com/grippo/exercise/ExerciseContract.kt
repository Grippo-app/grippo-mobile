package com.grippo.exercise

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseContract {
    fun onDismiss()
    fun onExampleDetailsClick(id: String)

    @Immutable
    companion object Empty : ExerciseContract {
        override fun onDismiss() {}
        override fun onExampleDetailsClick(id: String) {}
    }
}