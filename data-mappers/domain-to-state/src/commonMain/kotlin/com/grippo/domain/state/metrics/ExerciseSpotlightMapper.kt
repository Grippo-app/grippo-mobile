package com.grippo.domain.state.metrics

import com.grippo.domain.state.exercise.example.toState
import com.grippo.core.state.metrics.ExerciseSpotlightState as StateExerciseSpotlight
import com.grippo.data.features.api.metrics.models.ExerciseSpotlight as DomainExerciseSpotlight

public fun DomainExerciseSpotlight.MostConsistent.toState(): StateExerciseSpotlight.MostConsistentState {
    return StateExerciseSpotlight.MostConsistentState(
        example = example.toState(),
        totalVolume = totalVolume,
        sessionCount = sessionCount,
        trainingsCount = trainingsCount,
        coverageRatio = coverageRatio,
    )
}

public fun DomainExerciseSpotlight.BestProgress.toState(): StateExerciseSpotlight.BestProgressState {
    return StateExerciseSpotlight.BestProgressState(
        example = example.toState(),
        totalVolume = totalVolume,
        sessionCount = sessionCount,
        baselineVolumeMedian = baselineVolumeMedian,
        lastSessionVolume = lastSessionVolume,
        progressDelta = progressDelta,
        progressRatio = progressRatio,
    )
}

public fun DomainExerciseSpotlight.ComebackMissing.toState(): StateExerciseSpotlight.ComebackMissingState {
    return StateExerciseSpotlight.ComebackMissingState(
        example = example.toState(),
        totalVolume = totalVolume,
        sessionCount = sessionCount,
        typicalGap = typicalGap,
        currentGap = currentGap,
        score = score,
    )
}
