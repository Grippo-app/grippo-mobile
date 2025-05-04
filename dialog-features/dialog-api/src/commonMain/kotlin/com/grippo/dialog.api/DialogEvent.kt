package com.grippo.dialog.api

public sealed interface DialogEvent {
    public data class Show(val config: DialogConfig) : DialogEvent
    public data object Dismiss : DialogEvent
}