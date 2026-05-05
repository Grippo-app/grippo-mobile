package com.grippo.trainings

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.TrainingSeed

public sealed interface TrainingsRootDirection : BaseDirection {
    public data object Back : TrainingsRootDirection
    public data class EditTraining(val id: String) : TrainingsRootDirection
    public data class StartTraining(val seed: TrainingSeed) : TrainingsRootDirection
    public data object DraftTraining : TrainingsRootDirection
}
