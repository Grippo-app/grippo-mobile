package com.grippo.trainings

import com.grippo.core.foundation.models.BaseDirection

public sealed interface TrainingsRootDirection : BaseDirection {
    public data object Back : TrainingsRootDirection
    public data class ToEditTraining(val id: String) : TrainingsRootDirection
    public data object ToAddTraining : TrainingsRootDirection
    public data object ToDraftTraining : TrainingsRootDirection
}
