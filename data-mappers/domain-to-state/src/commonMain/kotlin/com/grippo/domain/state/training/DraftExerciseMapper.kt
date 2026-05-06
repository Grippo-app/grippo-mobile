package com.grippo.domain.state.training

import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.training.models.DraftExercise
import com.grippo.domain.state.exercise.example.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.uuid.Uuid

public fun List<DraftExercise>.toState(): PersistentList<ExerciseState> {
    return map { it.toState() }.toPersistentList()
}

public fun DraftExercise.toState(): ExerciseState {
    return ExerciseState(
        id = Uuid.random().toString(),
        name = name,
        iterations = iterations.toState(),
        exerciseExample = exerciseExample.toState(),
        createdAt = DateTimeFormatState.of(
            value = createdAt,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        ),
        total = TrainingTotalState(
            volume = VolumeFormatState.Empty(),
            repetitions = RepetitionsFormatState.Empty(),
            intensity = IntensityFormatState.Empty(),
        )
    )
}
