package com.grippo.trainings

import com.grippo.core.foundation.models.BaseDirection

public sealed interface TrainingsRootDirection : BaseDirection {
    public data object Back : TrainingsRootDirection
    public data class EditTraining(val id: String) : TrainingsRootDirection
    public data object StartTraining : TrainingsRootDirection
    public data object DraftTraining : TrainingsRootDirection
}
