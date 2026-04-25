package com.grippo.goal.setup.suggestion

import com.grippo.core.foundation.models.BaseDirection

public sealed interface GoalSetupSuggestionDirection : BaseDirection {
    public data object Configure : GoalSetupSuggestionDirection
    public data object Back : GoalSetupSuggestionDirection
}
