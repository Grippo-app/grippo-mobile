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
        repetitions = total.repetitions.value ?: return null,
        volume = total.volume.value ?: return null,
        intensity = total.intensity.value ?: return null
    )
}