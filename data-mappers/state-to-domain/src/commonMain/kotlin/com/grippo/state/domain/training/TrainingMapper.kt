package com.grippo.state.domain.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.state.trainings.TrainingState
import kotlinx.collections.immutable.toPersistentList

public fun List<TrainingState>.toDomain(): List<SetTraining> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun TrainingState.toDomain(): SetTraining? {
    return SetTraining(
        exercises = exercises.toDomain(),
        duration = duration
    )
}