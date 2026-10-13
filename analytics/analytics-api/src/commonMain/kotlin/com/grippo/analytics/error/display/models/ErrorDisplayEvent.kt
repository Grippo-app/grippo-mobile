package com.grippo.analytics.error.display.models

public sealed interface ErrorDisplayEvent {
    public data object DismissRequested : ErrorDisplayEvent
}
