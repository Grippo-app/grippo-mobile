package com.grippo.home.trainings

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface HomeTrainingsDirection : BaseDirection {
    data object Back : HomeTrainingsDirection
    data class EditTraining(val id: String) : HomeTrainingsDirection
    data object AddTraining : HomeTrainingsDirection

    data object ExcludedMuscles : HomeTrainingsDirection
    data object MissingEquipment : HomeTrainingsDirection
    data object WeightHistory : HomeTrainingsDirection
    data object Debug : HomeTrainingsDirection
}