package com.grippo.muscle.loading

import androidx.compose.runtime.Immutable

@Immutable
public interface MuscleLoadingContract {
    public fun onBack()
    public fun onSelectMode(value: MuscleLoadingShowingMode)

    public companion object Empty : MuscleLoadingContract {
        override fun onBack() {}
        override fun onSelectMode(value: MuscleLoadingShowingMode) {}
    }
}
