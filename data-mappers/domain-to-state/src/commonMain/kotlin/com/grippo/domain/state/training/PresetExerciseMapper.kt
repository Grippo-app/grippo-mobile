package com.grippo.domain.state.training

import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.training.models.PresetExercise
import com.grippo.domain.state.exercise.example.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.toPersistentList
import kotlin.uuid.Uuid

public fun PresetExercise.toState(): ExerciseState {
    return ExerciseState(
        id = Uuid.random().toString(),
        name = exerciseExample.name,
        iterations = iterations.map { it.toState() }.toPersistentList(),
        exerciseExample = exerciseExample.toState(),
        createdAt = DateTimeFormatState.of(
            value = DateTimeUtils.now(),
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