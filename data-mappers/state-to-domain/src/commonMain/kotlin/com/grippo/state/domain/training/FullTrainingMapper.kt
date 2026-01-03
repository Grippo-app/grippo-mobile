package com.grippo.state.domain.training

import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import com.grippo.state.domain.example.toDomain
import kotlinx.collections.immutable.toPersistentList

@Deprecated("drop it")
public fun List<TrainingState>.toDomainTrainings(): List<Training> {
    return mapNotNull { it.toDomainTraining() }.toPersistentList()
}

public fun TrainingState.toDomainTraining(): Training? {
    val exercises = exercises.mapNotNull { it.toDomainExercise() }
    val volume = total.volume.value ?: return null
    val repetitions = total.repetitions.value ?: return null
    val intensity = total.intensity.value ?: return null

    return Training(
        id = id,
        exercises = exercises,
        duration = duration,
        createdAt = createdAt,
        volume = volume,
        repetitions = repetitions,
        intensity = intensity,
    )
}

private fun ExerciseState.toDomainExercise(): Exercise? {
    val iterations = iterations.mapNotNull { it.toDomainIteration() }
    val volume = total.volume.value ?: return null
    val repetitions = total.repetitions.value ?: return null
    val intensity = total.intensity.value ?: return null

    return Exercise(
        id = id,
        name = name,
        iterations = iterations,
        exerciseExample = exerciseExample.toDomain() ?: return null,
        createdAt = createdAt,
        volume = volume,
        repetitions = repetitions,
        intensity = intensity,
    )
}

private fun IterationState.toDomainIteration(): Iteration? {
    val weight = volume.value ?: return null
    val reps = repetitions.value ?: return null
    return Iteration(
        id = id,
        volume = weight,
        repetitions = reps,
    )
}
