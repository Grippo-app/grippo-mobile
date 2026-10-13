package com.grippo.analytics.draft.training.models

public sealed interface DraftTrainingEvent {
    public data object ContinueClicked : DraftTrainingEvent
    public data object StartNewClicked : DraftTrainingEvent
    public data object DismissRequested : DraftTrainingEvent
}
