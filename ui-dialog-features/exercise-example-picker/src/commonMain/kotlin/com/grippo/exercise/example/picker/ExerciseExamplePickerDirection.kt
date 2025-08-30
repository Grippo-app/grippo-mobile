package com.grippo.exercise.example.picker

import com.grippo.core.models.BaseDirection
import com.grippo.state.exercise.examples.ExerciseExampleState

public sealed interface ExerciseExamplePickerDirection : BaseDirection {
    public data class BackWithResult(val value: ExerciseExampleState) :
        ExerciseExamplePickerDirection

    public data object Back :
        ExerciseExamplePickerDirection
}