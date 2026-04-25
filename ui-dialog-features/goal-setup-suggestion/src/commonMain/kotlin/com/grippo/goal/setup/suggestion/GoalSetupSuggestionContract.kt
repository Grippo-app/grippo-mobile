package com.grippo.goal.setup.suggestion

import androidx.compose.runtime.Immutable

@Immutable
internal interface GoalSetupSuggestionContract {
    fun onConfigure()
    fun onBack()

    @Immutable
    companion object Empty : GoalSetupSuggestionContract {
        override fun onConfigure() {}
        override fun onBack() {}
    }
}
