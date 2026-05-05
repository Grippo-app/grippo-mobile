package com.grippo.trainings.trainings

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.stage.TrainingSeed

internal sealed interface TrainingsDirection : BaseDirection {
    data object Back : TrainingsDirection
    data class EditTraining(val id: String) : TrainingsDirection
    data class StartTraining(val seed: TrainingSeed) : TrainingsDirection
}