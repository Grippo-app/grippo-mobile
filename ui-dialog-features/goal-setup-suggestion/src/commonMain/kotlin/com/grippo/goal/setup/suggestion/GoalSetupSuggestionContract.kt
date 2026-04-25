package com.grippo.goal.setup.suggestion

import androidx.compose.runtime.Immutable

@Immutable
internal interface GoalSetupSuggestionContract {
    fun onConfigure()
    fun onLater()
    fun onBack()

    @Immutable
    companion object Empty : GoalSetupSuggestionContract {
        override fun onConfigure() {}
        override fun onLater() {}
        override fun onBack() {}
    }
}
