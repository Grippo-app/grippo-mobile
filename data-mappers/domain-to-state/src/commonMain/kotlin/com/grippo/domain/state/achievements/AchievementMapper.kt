package com.grippo.domain.state.achievements

import com.grippo.core.state.achevements.AchievementState
import com.grippo.data.features.api.achievements.Achievement

public fun List<Achievement>.toState(): List<AchievementState> {
    return map(Achievement::toState)
}

public fun Achievement.toState(): AchievementState {
    return when (this) {
        is Achievement.BestTonnage -> AchievementState.BestTonnage(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            tonnage = tonnage
        )

        is Achievement.BestWeight -> AchievementState.BestWeight(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            iterationId = iterationId,
            weight = weight
        )

        is Achievement.LifetimeVolume -> AchievementState.LifetimeVolume(
            exerciseExampleId = exerciseExampleId,
            totalVolume = totalVolume,
            sessionsCount = sessionsCount
        )

        is Achievement.MaxRepetitions -> AchievementState.MaxRepetitions(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            iterationId = iterationId,
            totalVolume = totalVolume,
            repetitions = repetitions
        )

        is Achievement.PeakIntensity -> AchievementState.PeakIntensity(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            intensity = intensity
        )
    }
}
