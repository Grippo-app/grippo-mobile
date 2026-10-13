package com.grippo.domain.state.training

import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.PresetExercise
import com.grippo.domain.state.exercise.example.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.PersistentList
import kotlinx.datetime.LocalDateTime

public fun PresetExercise.toState(
    id: String,
    iterations: PersistentList<IterationState>,
    createdAt: LocalDateTime,
): ExerciseState {
    return ExerciseState(
        id = id,
        name = exerciseExample.name,
        iterations = iterations,
        exerciseExample = exerciseExample.toState(),
        createdAt = DateTimeFormatState.of(
            value = createdAt,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        ),
        total = TrainingTotalState(
            volume = VolumeFormatState.Empty(),
            repetitions = RepetitionsFormatState.Empty(),
            intensity = IntensityFormatState.Empty(),
        ),
    )
}