package com.grippo.training.profile.details

import androidx.compose.runtime.Immutable

@Immutable
public interface TrainingProfileDetailsContract {
    public fun onBack()

    public companion object Empty : TrainingProfileDetailsContract {
        override fun onBack() {}
    }
}
