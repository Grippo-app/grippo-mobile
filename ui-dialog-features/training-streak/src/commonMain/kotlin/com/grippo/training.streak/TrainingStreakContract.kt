package com.grippo.training.streak

import androidx.compose.runtime.Immutable

@Immutable
public interface TrainingStreakContract {
    public fun onBack()

    public companion object Empty : TrainingStreakContract {
        override fun onBack() {}
    }
}
