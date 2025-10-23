package com.grippo.dto.domain.training

import com.grippo.backend.dto.training.ExerciseResponse
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.toolkit.logger.AppLogger

public fun List<ExerciseResponse>.toDomain(example: ExerciseExampleValue): List<Exercise> {
    return mapNotNull { it.toDomainOrNull(example) }
}

public fun ExerciseResponse.toDomainOrNull(example: ExerciseExampleValue): Exercise? {
    val domainId = AppLogger.Mapping.log(this@toDomainOrNull.id) {
        "ExerciseResponse.id is null"
    } ?: return null

    val domainName = AppLogger.Mapping.log(name) {
        "ExerciseResponse.name is null"
    } ?: return null

    val domainVolume = AppLogger.Mapping.log(volume) {
        "ExerciseResponse.volume is null"
    } ?: return null

    val domainRepetitions = AppLogger.Mapping.log(repetitions) {
        "ExerciseResponse.repetitions is null"
    } ?: return null

    val domainIntensity = AppLogger.Mapping.log(intensity) {
        "ExerciseResponse.intensity is null"
    } ?: return null

    return Exercise(
        id = domainId,
        name = domainName,
        volume = domainVolume,
        repetitions = domainRepetitions,
        intensity = domainIntensity,
        iterations = iterations.toDomain(),
        exerciseExample = example
    )
}