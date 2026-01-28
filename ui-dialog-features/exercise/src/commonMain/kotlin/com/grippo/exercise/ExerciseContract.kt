package com.grippo.exercise

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseContract {
    fun onDismiss()
    fun onExampleDetailsClick()

    @Immutable
    companion object Empty : ExerciseContract {
        override fun onDismiss() {}
        override fun onExampleDetailsClick() {}
    }
}