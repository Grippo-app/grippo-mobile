package com.grippo.state.domain.training

import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.state.domain.example.toDomain
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseState>.toDomain(): List<SetExercise> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun ExerciseState.toDomain(): SetExercise? {
    val mappedRepetitions = AppLogger.Mapping.log(total.repetitions.value) {
        "ExerciseState total.repetitions value is null (id=$id)"
    } ?: return null

    val mappedVolume = AppLogger.Mapping.log(total.volume.value) {
        "ExerciseState total.volume value is null (id=$id)"
    } ?: return null

    val mappedIntensity = AppLogger.Mapping.log(total.intensity.value) {
        "ExerciseState total.intensity value is null (id=$id)"
    } ?: return null

    val mappedCreatedAt = AppLogger.Mapping.log(createdAt.value) {
        "ExerciseState createdAt value is null (id=$id)"
    } ?: return null

    return SetExercise(
        name = name,
        iterations = iterations.toDomain(),
        exerciseExample = exerciseExample.toDomain(),
        repetitions = mappedRepetitions,
        volume = mappedVolume,
        intensity = mappedIntensity,
        createdAt = mappedCreatedAt,
    )
}
