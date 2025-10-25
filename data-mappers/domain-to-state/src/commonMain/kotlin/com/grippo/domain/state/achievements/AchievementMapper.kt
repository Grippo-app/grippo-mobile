package com.grippo.domain.state.achievements

import com.grippo.core.state.achevements.AchievementState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.data.features.api.achievements.Achievement

public fun List<Achievement>.toState(): List<AchievementState> {
    return map(Achievement::toState)
}

public fun Achievement.toState(): AchievementState {
    return when (this) {
        is Achievement.BestTonnage -> AchievementState.BestTonnage(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            tonnage = VolumeFormatState.of(tonnage.toFloat()),
        )

        is Achievement.BestWeight -> AchievementState.BestWeight(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            iterationId = iterationId,
            weight = VolumeFormatState.of(weight.toFloat()),
        )

        is Achievement.LifetimeVolume -> AchievementState.LifetimeVolume(
            exerciseExampleId = exerciseExampleId,
            totalVolume = VolumeFormatState.of(totalVolume.toFloat()),
            sessionsCount = sessionsCount
        )

        is Achievement.MaxRepetitions -> AchievementState.MaxRepetitions(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            iterationId = iterationId,
            totalVolume = VolumeFormatState.of(totalVolume.toFloat()),
            repetitions = RepetitionsFormatState.of(repetitions)
        )

        is Achievement.PeakIntensity -> AchievementState.PeakIntensity(
            exerciseExampleId = exerciseExampleId,
            exerciseId = exerciseId,
            intensity = IntensityFormatState.of(intensity.toFloat())
        )
    }
}
