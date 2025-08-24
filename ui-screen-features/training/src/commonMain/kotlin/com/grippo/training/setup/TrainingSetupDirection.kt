package com.grippo.training.setup

import com.grippo.core.models.BaseDirection

internal sealed interface TrainingSetupDirection : BaseDirection {
    data class ToRecording(val selectedMuscleIds: List<String>) : TrainingSetupDirection
    data object Back : TrainingSetupDirection
}
