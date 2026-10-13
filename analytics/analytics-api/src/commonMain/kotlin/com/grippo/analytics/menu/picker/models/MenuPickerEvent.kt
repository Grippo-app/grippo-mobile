package com.grippo.analytics.menu.picker.models

public sealed interface MenuPickerEvent {
    public data object ItemClicked : MenuPickerEvent
    public data object DismissRequested : MenuPickerEvent
}
