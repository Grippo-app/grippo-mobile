package com.grippo.training.profile

import androidx.compose.runtime.Immutable

@Immutable
public interface TrainingProfileContract {
    public fun onBack()

    public companion object Empty : TrainingProfileContract {
        override fun onBack() {}
    }
}
