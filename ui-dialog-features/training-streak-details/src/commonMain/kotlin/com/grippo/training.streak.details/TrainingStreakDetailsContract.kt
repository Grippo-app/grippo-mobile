package com.grippo.training.streak.details

import androidx.compose.runtime.Immutable

@Immutable
public interface TrainingStreakDetailsContract {
    public fun onBack()

    public companion object Empty : TrainingStreakDetailsContract {
        override fun onBack() {}
    }
}
