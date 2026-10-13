package com.grippo.analytics.start.training.models

public sealed interface StartTrainingEvent {
    public data class OptionConfirmed(val option: StartTrainingOptionKind) : StartTrainingEvent
    public data object Back : StartTrainingEvent
}
