package com.grippo.exercises

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExercisesRootContract {
    fun onBack()

    @Immutable
    companion object Empty : ExercisesRootContract {
        override fun onBack() {}
    }
}
