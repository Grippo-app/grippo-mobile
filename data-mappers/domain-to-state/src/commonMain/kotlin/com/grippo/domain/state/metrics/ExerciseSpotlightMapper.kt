package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.domain.state.exercise.example.toState
import com.grippo.core.state.metrics.ExerciseSpotlight as StateExerciseSpotlight
import com.grippo.data.features.api.metrics.models.ExerciseSpotlight as DomainExerciseSpotlight

public fun DomainExerciseSpotlight?.toState(): StateExerciseSpotlight? {
    return this?.let { focus ->
        StateExerciseSpotlight(
            exercise = focus.example.toState(),
            totalVolume = VolumeFormatState.of(focus.totalVolume),
            sessionCount = focus.sessionCount,
        )
    }
}
