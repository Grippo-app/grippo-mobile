package com.grippo.muscle.loading.details

import androidx.compose.runtime.Immutable

@Immutable
public interface MuscleLoadingDetailsContract {
    public fun onBack()
    public fun onSelectMode(value: MuscleLoadingDetailsShowingMode)

    public companion object Empty : MuscleLoadingDetailsContract {
        override fun onBack() {}
        override fun onSelectMode(value: MuscleLoadingDetailsShowingMode) {}
    }
}
