package com.grippo.exercises

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExercisesContract {
    fun onExampleClick(id: String)

    @Immutable
    companion object Empty : ExercisesContract {
        override fun onExampleClick(id: String) {}
    }
}
