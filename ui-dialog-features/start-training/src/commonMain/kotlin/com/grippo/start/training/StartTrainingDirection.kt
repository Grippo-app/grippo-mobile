package com.grippo.start.training

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList

public sealed interface StartTrainingDirection : BaseDirection {
    public data object StartEmpty : StartTrainingDirection
    public data class UseExercises(val exercises: ImmutableList<ExerciseState>) : StartTrainingDirection
    public data object Back : StartTrainingDirection
}
