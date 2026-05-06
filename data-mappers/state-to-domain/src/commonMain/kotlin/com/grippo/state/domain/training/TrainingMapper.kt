package com.grippo.state.domain.training

import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.toPersistentList

public fun List<TrainingState>.toDomain(): List<SetTraining> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun TrainingState.toDomain(): SetTraining? {
    val mappedDuration = AppLogger.Mapping.log(duration.value) {
        "TrainingState duration value is null (id=$id)"
    } ?: return null

    val mappedRepetitions = AppLogger.Mapping.log(total.repetitions.value) {
        "TrainingState total.repetitions value is null (id=$id)"
    } ?: return null

    val mappedVolume = AppLogger.Mapping.log(total.volume.value) {
        "TrainingState total.volume value is null (id=$id)"
    } ?: return null

    val mappedIntensity = AppLogger.Mapping.log(total.intensity.value) {
        "TrainingState total.intensity value is null (id=$id)"
    } ?: return null

    return SetTraining(
        exercises = exercises.toDomain(),
        duration = mappedDuration,
        repetitions = mappedRepetitions,
        volume = mappedVolume,
        intensity = mappedIntensity,
    )
}
