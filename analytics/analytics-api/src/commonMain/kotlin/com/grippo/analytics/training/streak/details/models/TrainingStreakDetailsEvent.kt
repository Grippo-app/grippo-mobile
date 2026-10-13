package com.grippo.analytics.training.streak.details.models

public sealed interface TrainingStreakDetailsEvent {
    public data object DismissRequested : TrainingStreakDetailsEvent
}
