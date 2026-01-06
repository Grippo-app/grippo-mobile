package com.grippo.muscle.loading

import androidx.compose.runtime.Immutable

@Immutable
public interface MuscleLoadingContract {
    public fun onBack()

    public companion object Empty : MuscleLoadingContract {
        override fun onBack() {}
    }
}
