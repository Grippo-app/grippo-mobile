package com.grippo.training.goal.details

import androidx.compose.runtime.Immutable

@Immutable
public interface TrainingGoalDetailsContract {
    public fun onBack()

    public companion object Empty : TrainingGoalDetailsContract {
        override fun onBack() {}
    }
}
