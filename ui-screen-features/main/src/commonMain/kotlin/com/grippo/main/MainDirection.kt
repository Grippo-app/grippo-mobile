package com.grippo.main

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.StageState
import com.grippo.screen.api.MainRouter

public sealed interface MainDirection : BaseDirection {
    public data class SelectTab(val tab: MainRouter) : MainDirection
    public data class Training(val stage: StageState) : MainDirection
    public data object Goal : MainDirection
    public data object Back : MainDirection
}
