package com.grippo.training.preferences

import com.grippo.core.models.BaseDirection

public sealed interface TrainingPreferencesDirection : BaseDirection {
    public data class ToRecording(val selectedMuscleIds: List<String>) : TrainingPreferencesDirection
    public data object Back : TrainingPreferencesDirection
}