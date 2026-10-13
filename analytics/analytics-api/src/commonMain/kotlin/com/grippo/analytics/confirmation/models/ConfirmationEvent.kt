package com.grippo.analytics.confirmation.models

public sealed interface ConfirmationEvent {
    public data object ConfirmClicked : ConfirmationEvent
    public data object DismissRequested : ConfirmationEvent
}
