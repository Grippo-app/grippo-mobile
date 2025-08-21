package com.grippo.shared.root

import com.grippo.core.models.BaseDirection

public sealed interface RootDirection : BaseDirection {
    public data object Login : RootDirection
    public data object Close : RootDirection
    public data object ToHome : RootDirection
    public data object ToProfile : RootDirection
    public data object ToDebug : RootDirection
    public data object ToSettings : RootDirection
    public data object ToWorkout : RootDirection
    public data object ToWeightHistory : RootDirection
    public data object ToMissingEquipment : RootDirection
    public data object ToExcludedMuscles : RootDirection
    public data object ToExerciseExamples : RootDirection
    public data object ToSystemSettings : RootDirection
    public data object Back : RootDirection
}