package com.grippo.domain.state.training

import com.grippo.data.features.api.training.models.Training
import com.grippo.state.trainings.TrainingState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<Training>.toState(): PersistentList<TrainingState> {
    return map { it.toState() }.toPersistentList()
}

public fun Training.toState(): TrainingState {
    return TrainingState(
        id = id,
        exercises = exercises.toState(),
        volume = volume,
        repetitions = repetitions,
        intensity = intensity,
        duration = duration,
        createdAt = createdAt
    )
}