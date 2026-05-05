package com.grippo.core.state.achievements

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
public sealed interface AchievementState {

    public val key: String
    public val exerciseExampleId: String

    @Immutable
    public data class BestTonnage(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val tonnage: VolumeFormatState
    ) : AchievementState {
        override val key: String get() = KEY

        public companion object {
            public const val KEY: String = "best_tonnage"
        }
    }

    @Immutable
    public data class BestWeight(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val weight: VolumeFormatState
    ) : AchievementState {
        override val key: String get() = KEY

        public companion object {
            public const val KEY: String = "best_weight"
        }
    }

    @Immutable
    public data class LifetimeVolume(
        override val exerciseExampleId: String,
        val totalVolume: VolumeFormatState,
        val sessionsCount: Int
    ) : AchievementState {
        override val key: String get() = KEY

        public companion object {
            public const val KEY: String = "lifetime_volume"
        }
    }

    @Immutable
    public data class MaxRepetitions(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val totalVolume: VolumeFormatState,
        val repetitions: RepetitionsFormatState
    ) : AchievementState {
        override val key: String get() = KEY

        public companion object {
            public const val KEY: String = "max_repetitions"
        }
    }

    @Immutable
    public data class PeakIntensity(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val intensity: IntensityFormatState,
    ) : AchievementState {
        override val key: String get() = KEY

        public companion object {
            public const val KEY: String = "peak_intensity"
        }
    }

    public fun text(): UiText = when (this) {
        is BestTonnage -> TEXT_BEST_TONNAGE
        is BestWeight -> TEXT_BEST_WEIGHT
        is LifetimeVolume -> TEXT_LIFETIME_VOLUME
        is MaxRepetitions -> TEXT_MAX_REPETITIONS
        is PeakIntensity -> TEXT_PEAK_INTENSITY
    }

    @Composable
    public fun color(): Color = when (this) {
        is BestTonnage -> AppTokens.colors.training.volume.startColor
        is BestWeight -> AppTokens.colors.training.volume.startColor
        is LifetimeVolume -> AppTokens.colors.training.volume.startColor
        is MaxRepetitions -> AppTokens.colors.training.repetitions.startColor
        is PeakIntensity -> AppTokens.colors.training.intensity.startColor
    }

    @Composable
    public fun value(): String = when (this) {
        is BestTonnage -> this.tonnage.short()
        is BestWeight -> this.weight.short()
        is LifetimeVolume -> this.totalVolume.short()
        is MaxRepetitions -> this.repetitions.short()
        is PeakIntensity -> this.intensity.short()
    }

    public companion object {
        private val TEXT_BEST_TONNAGE: UiText = UiText.Res(Res.string.best_tonnage)
        private val TEXT_BEST_WEIGHT: UiText = UiText.Res(Res.string.best_weight)
        private val TEXT_LIFETIME_VOLUME: UiText = UiText.Res(Res.string.lifetime_volume)
        private val TEXT_MAX_REPETITIONS: UiText = UiText.Res(Res.string.max_repetitions)
        private val TEXT_PEAK_INTENSITY: UiText = UiText.Res(Res.string.peak_intensity)
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
