package com.grippo.core.state.achevements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.best_tonnage
import com.grippo.design.resources.provider.best_weight
import com.grippo.design.resources.provider.lifetime_volume
import com.grippo.design.resources.provider.max_repetitions
import com.grippo.design.resources.provider.peak_intensity
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public sealed class AchievementState(
    public val key: String,
    public open val exerciseExampleId: String
) {
    public data class BestTonnage(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val tonnage: VolumeFormatState
    ) : AchievementState(
        key = "best_tonnage",
        exerciseExampleId = exerciseExampleId,
    )

    public data class BestWeight(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val weight: VolumeFormatState
    ) : AchievementState(
        key = "best_weight",
        exerciseExampleId = exerciseExampleId,
    )

    public data class LifetimeVolume(
        override val exerciseExampleId: String,
        val totalVolume: VolumeFormatState,
        val sessionsCount: Int
    ) : AchievementState(
        key = "lifetime_volume",
        exerciseExampleId = exerciseExampleId,
    )

    public data class MaxRepetitions(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val totalVolume: VolumeFormatState,
        val repetitions: RepetitionsFormatState
    ) : AchievementState(
        key = "max_repetitions",
        exerciseExampleId = exerciseExampleId,
    )

    public data class PeakIntensity(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val intensity: IntensityFormatState,
    ) : AchievementState(
        key = "peak_intensity",
        exerciseExampleId = exerciseExampleId,
    )

    public fun text(): UiText {
        return when (this) {
            is BestTonnage -> UiText.Res(Res.string.best_tonnage)
            is BestWeight -> UiText.Res(Res.string.best_weight)
            is LifetimeVolume -> UiText.Res(Res.string.lifetime_volume)
            is MaxRepetitions -> UiText.Res(Res.string.max_repetitions)
            is PeakIntensity -> UiText.Res(Res.string.peak_intensity)
        }
    }

    @Composable
    public fun color1(): Color {
        return when (this) {
            is BestTonnage -> AppTokens.colors.achievements.bestTonnage1
            is BestWeight -> AppTokens.colors.achievements.bestWeight1
            is LifetimeVolume -> AppTokens.colors.achievements.lifetimeVolume1
            is MaxRepetitions -> AppTokens.colors.achievements.maxRepetitions1
            is PeakIntensity -> AppTokens.colors.achievements.peakIntensity1
        }
    }

    @Composable
    public fun color2(): Color {
        return when (this) {
            is BestTonnage -> AppTokens.colors.achievements.bestTonnage2
            is BestWeight -> AppTokens.colors.achievements.bestWeight2
            is LifetimeVolume -> AppTokens.colors.achievements.lifetimeVolume2
            is MaxRepetitions -> AppTokens.colors.achievements.maxRepetitions2
            is PeakIntensity -> AppTokens.colors.achievements.peakIntensity2
        }
    }

    @Composable
    public fun value(): String {
        return when (this) {
            is BestTonnage -> this.tonnage.short()
            is BestWeight -> this.weight.short()
            is LifetimeVolume -> this.totalVolume.short()
            is MaxRepetitions -> this.repetitions.short()
            is PeakIntensity -> this.intensity.short()
        }
    }
}

public fun stubAchievements(): PersistentList<AchievementState> = persistentListOf(
    stubBestTonnageAchievement(),
    stubBestWeightAchievement(),
    stubLifetimeVolumeAchievement(),
    stubMaxRepetitionsAchievement(),
    stubPeakIntensityAchievement(),
)

public fun stubAchievement(): AchievementState {
    return when (Random.nextInt(0, 5)) {
        0 -> stubBestTonnageAchievement()
        1 -> stubBestWeightAchievement()
        2 -> stubLifetimeVolumeAchievement()
        3 -> stubMaxRepetitionsAchievement()
        else -> stubPeakIntensityAchievement()
    }
}

private fun stubBestTonnageAchievement(): AchievementState.BestTonnage {
    return AchievementState.BestTonnage(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        tonnage = VolumeFormatState.of(Random.nextInt(1_000, 12_000).toFloat())
    )
}

private fun stubBestWeightAchievement(): AchievementState.BestWeight {
    return AchievementState.BestWeight(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        iterationId = Uuid.random().toString(),
        weight = VolumeFormatState.of(Random.nextInt(20, 280).toFloat())
    )
}

private fun stubLifetimeVolumeAchievement(): AchievementState.LifetimeVolume {
    return AchievementState.LifetimeVolume(
        exerciseExampleId = Uuid.random().toString(),
        totalVolume = VolumeFormatState.of(Random.nextInt(5_000, 50_000).toFloat()),
        sessionsCount = Random.nextInt(10, 120)
    )
}

private fun stubMaxRepetitionsAchievement(): AchievementState.MaxRepetitions {
    return AchievementState.MaxRepetitions(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        iterationId = Uuid.random().toString(),
        totalVolume = VolumeFormatState.of(Random.nextInt(500, 10_000).toFloat()),
        repetitions = RepetitionsFormatState.of(Random.nextInt(3, 25))
    )
}

private fun stubPeakIntensityAchievement(): AchievementState.PeakIntensity {
    return AchievementState.PeakIntensity(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        intensity = IntensityFormatState.of(Random.nextDouble(0.75, 2.5).toFloat())
    )
}