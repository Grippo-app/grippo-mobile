package com.grippo.domain.state.training

import com.grippo.data.features.api.training.models.Training
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.TrainingMetrics
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
        metrics = TrainingMetrics(
            volume = VolumeFormatState.of(volume),
            repetitions = RepetitionsFormatState.of(repetitions),
            intensity = IntensityFormatState.of(intensity),
        ),
        duration = duration,
        createdAt = createdAt
    )
}