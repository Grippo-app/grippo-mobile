package com.grippo.shared.root

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.StageState
import com.grippo.screen.api.ExercisesRouter

public sealed interface RootDirection : BaseDirection {
    public data object Login : RootDirection
    public data object Close : RootDirection
    public data object Home : RootDirection
    public data object Debug : RootDirection
    public data class Training(val stage: StageState) : RootDirection
    public data class Exercises(val route: ExercisesRouter.Library) : RootDirection
    public data object WeightHistory : RootDirection
    public data object Goal : RootDirection
    public data object Back : RootDirection
}
