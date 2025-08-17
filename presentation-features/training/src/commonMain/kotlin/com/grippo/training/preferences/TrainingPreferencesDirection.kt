package com.grippo.training.preferences

import com.grippo.core.models.BaseDirection

internal sealed interface TrainingPreferencesDirection : BaseDirection {
    data class ToRecording(val selectedMuscleIds: List<String>) : TrainingPreferencesDirection
    data object Back : TrainingPreferencesDirection
}