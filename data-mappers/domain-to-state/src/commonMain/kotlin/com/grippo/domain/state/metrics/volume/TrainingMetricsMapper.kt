package com.grippo.domain.state.metrics.volume

import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState as StateTrainingMetrics
import com.grippo.data.features.api.metrics.volume.models.TrainingTotal as DomainTrainingMetrics

public fun DomainTrainingMetrics.toState(): StateTrainingMetrics {
    return StateTrainingMetrics(
        volume = VolumeFormatState.ofTotal(volume),
        repetitions = RepetitionsFormatState.ofTotal(repetitions),
        intensity = IntensityFormatState.ofAverage(intensity),
    )
}
