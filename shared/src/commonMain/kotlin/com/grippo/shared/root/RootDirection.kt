package com.grippo.shared.root

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.state.stage.StageState

public sealed interface RootDirection : BaseDirection {
    public data object Login : RootDirection
    public data object Close : RootDirection
    public data object ToHome : RootDirection
    public data object ToProfile : RootDirection
    public data object ToDebug : RootDirection
    public data class ToTraining(val stage: StageState) : RootDirection
    public data object ToWeightHistory : RootDirection
    public data object ToMissingEquipment : RootDirection
    public data object ToExcludedMuscles : RootDirection
    public data object Back : RootDirection
}