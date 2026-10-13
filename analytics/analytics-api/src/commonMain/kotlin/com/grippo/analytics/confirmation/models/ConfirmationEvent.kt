package com.grippo.analytics.confirmation.models

public sealed interface ConfirmationEvent {
    public data object Confirm : ConfirmationEvent
    public data object Back : ConfirmationEvent
}
