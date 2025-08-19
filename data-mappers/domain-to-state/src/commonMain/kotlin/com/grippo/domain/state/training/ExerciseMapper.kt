package com.grippo.domain.state.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.domain.state.exercise.example.toState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<Exercise>.toState(): PersistentList<ExerciseState> {
    return map { it.toState() }.toPersistentList()
}

public fun Exercise.toState(): ExerciseState {
    return ExerciseState(
        id = id,
        name = name,
        iterations = iterations.toState(),
        volume = VolumeFormatState.of(volume),
        repetitions = RepetitionsFormatState.of(repetitions),
        intensity = IntensityFormatState.of(intensity),
        exerciseExample = exerciseExample?.toState()
    )
}