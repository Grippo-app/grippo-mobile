package com.grippo.analytics.muscle.loading.details.models

public sealed interface MuscleLoadingDetailsEvent {
    public data class ModeSelected(val mode: MuscleLoadingDetailsMode) : MuscleLoadingDetailsEvent
    public data object DismissRequested : MuscleLoadingDetailsEvent
}
