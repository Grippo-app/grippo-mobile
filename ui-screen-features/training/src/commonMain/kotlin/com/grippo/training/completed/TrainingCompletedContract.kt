package com.grippo.training.completed

import androidx.compose.runtime.Immutable

@Immutable
internal interface TrainingCompletedContract {
    fun onExerciseClick(id: String)
    fun onSummaryClick()
    fun onBack()

    @Immutable
    companion object Empty : TrainingCompletedContract {
        override fun onExerciseClick(id: String) {}
        override fun onSummaryClick() {}
        override fun onBack() {}
    }
}