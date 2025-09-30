package com.grippo.home.trainings

import com.grippo.core.models.BaseDirection

internal sealed interface HomeTrainingsDirection : BaseDirection {
    data object Back : HomeTrainingsDirection
    data class EditTraining(val id: String) : HomeTrainingsDirection
}