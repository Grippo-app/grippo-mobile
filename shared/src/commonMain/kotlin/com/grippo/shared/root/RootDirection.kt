package com.grippo.shared.root

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.StageState

public sealed interface RootDirection : BaseDirection {
    public data object Login : RootDirection
    public data object Close : RootDirection
    public data object Home : RootDirection
    public data object Profile : RootDirection
    public data object Debug : RootDirection
    public data object Trainings : RootDirection
    public data class Training(val stage: StageState) : RootDirection
    public data object WeightHistory : RootDirection
    public data object MissingEquipment : RootDirection
    public data object ExcludedMuscles : RootDirection
    public data object Experience : RootDirection
    public data object Settings : RootDirection
    public data object Social : RootDirection
    public data object Back : RootDirection
}
