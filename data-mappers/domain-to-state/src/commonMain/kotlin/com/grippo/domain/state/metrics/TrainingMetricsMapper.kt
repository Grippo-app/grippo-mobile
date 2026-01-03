package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.TrainingTotalState as StateTrainingMetrics
import com.grippo.data.features.api.metrics.models.TrainingMetrics as DomainTrainingMetrics

public fun DomainTrainingMetrics.toState(): StateTrainingMetrics {
    return StateTrainingMetrics(
        volume = VolumeFormatState.of(volume),
        repetitions = RepetitionsFormatState.of(repetitions),
        intensity = IntensityFormatState.of(intensity),
    )
}
