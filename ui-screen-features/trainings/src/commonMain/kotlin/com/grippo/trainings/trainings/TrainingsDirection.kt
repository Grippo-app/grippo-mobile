package com.grippo.trainings.trainings

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface TrainingsDirection : BaseDirection {
    data object Back : TrainingsDirection
    data class EditTraining(val id: String) : TrainingsDirection
    data object AddTraining : TrainingsDirection
}