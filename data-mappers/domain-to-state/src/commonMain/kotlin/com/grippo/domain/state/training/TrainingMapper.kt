package com.grippo.domain.state.training

import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.TrainingMetricsState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.data.features.api.training.models.Training
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<Training>.toState(): PersistentList<TrainingState> {
    return map { it.toState() }.toPersistentList()
}

public fun Training.toState(): TrainingState {
    return TrainingState(
        id = id,
        exercises = exercises.toState(),
        metrics = TrainingMetricsState(
            volume = VolumeFormatState.of(volume),
            repetitions = RepetitionsFormatState.of(repetitions),
            intensity = IntensityFormatState.of(intensity),
        ),
        duration = duration,
        createdAt = createdAt
    )
}
