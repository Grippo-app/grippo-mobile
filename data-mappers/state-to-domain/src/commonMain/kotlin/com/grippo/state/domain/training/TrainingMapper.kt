package com.grippo.state.domain.training

import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.training.models.SetTraining
import kotlinx.collections.immutable.toPersistentList

public fun List<TrainingState>.toDomain(): List<SetTraining> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun TrainingState.toDomain(): SetTraining? {
    return SetTraining(
        exercises = exercises.toDomain(),
        duration = duration,
        repetitions = metrics.repetitions.value ?: return null,
        volume = metrics.volume.value ?: return null,
        intensity = metrics.intensity.value ?: return null
    )
}