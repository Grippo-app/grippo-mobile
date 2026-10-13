package com.grippo.analytics.main.models

public sealed interface MainEvent {
    public data class TabSelected(public val tab: MainTab) : MainEvent
    public data object StartTrainingClicked : MainEvent
}
