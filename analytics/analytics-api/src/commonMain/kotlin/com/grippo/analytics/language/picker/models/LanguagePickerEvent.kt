package com.grippo.analytics.language.picker.models

public sealed interface LanguagePickerEvent {
    public data object LanguageClicked : LanguagePickerEvent
    public data object DismissRequested : LanguagePickerEvent
}
